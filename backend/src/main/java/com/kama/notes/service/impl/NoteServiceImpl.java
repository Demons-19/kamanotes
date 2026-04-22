package com.kama.notes.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.kama.notes.annotation.NeedLogin;
import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.mapper.QuestionMapper;
import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.base.EmptyVO;
import com.kama.notes.model.base.Pagination;
import com.kama.notes.model.dto.note.CreateNoteRequest;
import com.kama.notes.model.dto.note.NoteQueryParams;
import com.kama.notes.model.dto.note.UpdateNoteRequest;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.entity.Question;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.enums.redisKey.RedisKey;
import com.kama.notes.model.vo.category.CategoryVO;
import com.kama.notes.model.vo.note.*;
import com.kama.notes.scope.RequestScopeData;
import com.kama.notes.service.*;
import com.kama.notes.utils.ApiResponseUtil;
import com.kama.notes.utils.MarkdownUtil;
import com.kama.notes.utils.PaginationUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class NoteServiceImpl implements NoteService {
    @Autowired private NoteMapper noteMapper;
    @Autowired private UserService userService;
    @Autowired private QuestionService questionService;
    @Autowired private NoteLikeService noteLikeService;
    @Autowired private NoteHotRankService noteHotRankService;
    @Autowired private CollectionNoteService collectionNoteService;
    @Autowired private RequestScopeData requestScopeData;
    @Autowired private CategoryService categoryService;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private RedisService redisService;
    @Autowired private Cache<String, Object> noteLocalCache;
    @Autowired private NoteReviewQueueService noteReviewQueueService;
    private static final String LK="note:like_count:",CK="note:collect_count:",MK="note:comment_count:";

    @Override
    public ApiResponse<List<NoteVO>> getNotes(NoteQueryParams p){
        int offset= PaginationUtils.calculateOffset(p.getPage(),p.getPageSize());
        int total=noteMapper.countNotes(p);
        List<Note> notes=noteMapper.findByQueryParams(p,offset,p.getPageSize());
        fillRealtimeCounts(notes);
        List<Integer> qids=notes.stream().map(Note::getQuestionId).distinct().toList();
        List<Long> aids=notes.stream().map(Note::getAuthorId).distinct().toList();
        List<Integer> nids=notes.stream().map(Note::getNoteId).toList();
        Map<Long,User> users=userService.getUserMapByIds(aids);
        Map<Integer,Question> qs=questionService.getQuestionMapByIds(qids);
        Set<Integer> likedTmp=Collections.emptySet();
        Set<Integer> collectedTmp=Collections.emptySet();
        if(requestScopeData.isLogin()&&requestScopeData.getUserId()!=null){Long uid=requestScopeData.getUserId();likedTmp=noteLikeService.findUserLikedNoteIds(uid,nids);collectedTmp=collectionNoteService.findUserCollectedNoteIds(uid,nids);}        
        final Set<Integer> liked=likedTmp;
        final Set<Integer> collected=collectedTmp;
        List<NoteVO> data=notes.stream().map(n->toNoteVO(n,users,qs,liked,collected)).toList();
        return ApiResponseUtil.success("获取笔记列表成功",data,new Pagination(p.getPage(),p.getPageSize(),total));
    }

    @Override
    public ApiResponse<NoteVO> getNoteDetail(Integer noteId){
        String localKey="note:detail:"+noteId, redisKey=RedisKey.noteDetail(noteId);
        NoteVO cached=(NoteVO) noteLocalCache.getIfPresent(localKey);
        if(cached!=null)return ApiResponseUtil.success("获取笔记详情成功",fillUserActions(cached));
        Object redisObj=redisService.get(redisKey);
        if(redisObj instanceof NoteVO vo){noteLocalCache.put(localKey,vo);return ApiResponseUtil.success("获取笔记详情成功",fillUserActions(vo));}
        if(redisObj!=null)return ApiResponseUtil.error("笔记不存在");
        Note note=noteMapper.findById(noteId);
        if(note==null){redisService.setWithExpiry(redisKey,new EmptyVO(),300);return ApiResponseUtil.error("笔记不存在");}
        NoteVO base=buildBaseNoteVO(note);
        redisService.setWithExpiry(redisKey,base,1800+new Random().nextInt(600));
        noteLocalCache.put(localKey,base);
        return ApiResponseUtil.success("获取笔记详情成功",fillUserActions(base));
    }

    @Override @NeedLogin
    public ApiResponse<CreateNoteVO> createNote(CreateNoteRequest r){
        Long uid=requestScopeData.getUserId();
        if(questionService.findById(r.getQuestionId())==null)return ApiResponseUtil.error("questionId 对应的问题不存在");
        Note note=new Note();BeanUtils.copyProperties(r,note);note.setAuthorId(uid);note.setStatus(0);note.setReviewRetryCount(0);
        try{noteMapper.insert(note);noteReviewQueueService.enqueue(note.getNoteId(),uid);CreateNoteVO vo=new CreateNoteVO();vo.setNoteId(note.getNoteId());return ApiResponseUtil.success("笔记提交成功，正在审核中",vo);}catch(Exception e){log.error("创建笔记失败",e);return ApiResponseUtil.error("创建笔记失败");}
    }

    @Override @NeedLogin
    public ApiResponse<EmptyVO> updateNote(Integer noteId, UpdateNoteRequest r){
        Note note=noteMapper.findRawById(noteId);
        if(note==null)return ApiResponseUtil.error("笔记不存在");
        if(!Objects.equals(requestScopeData.getUserId(),note.getAuthorId()))return ApiResponseUtil.error("没有权限修改别人的笔记");
        try{note.setContent(r.getContent());noteMapper.update(note);evictNoteDetailCache(noteId);return ApiResponseUtil.success("更新笔记成功");}catch(Exception e){return ApiResponseUtil.error("更新笔记失败");}
    }

    @Override @NeedLogin
    public ApiResponse<EmptyVO> deleteNote(Integer noteId){
        Note note=noteMapper.findRawById(noteId);
        if(note==null)return ApiResponseUtil.error("笔记不存在");
        if(!Objects.equals(requestScopeData.getUserId(),note.getAuthorId()))return ApiResponseUtil.error("没有权限删除别人的笔记");
        try{noteMapper.deleteById(noteId);removeNoteFromHotRank(noteId);evictNoteDetailCache(noteId);return ApiResponseUtil.success("删除笔记成功");}catch(Exception e){return ApiResponseUtil.error("删除笔记失败");}
    }

    @Override @NeedLogin
    public ApiResponse<DownloadNoteVO> downloadNote(){
        Long uid=requestScopeData.getUserId();List<Note> notes=noteMapper.findByAuthorId(uid);if(notes.isEmpty())return ApiResponseUtil.error("不存在任何笔记");
        Map<Integer,Note> noteMap=notes.stream().collect(Collectors.toMap(Note::getQuestionId,n->n));List<Question> questions=questionMapper.findByIdBatch(notes.stream().map(Note::getQuestionId).toList());StringBuilder md=new StringBuilder();
        for(CategoryVO c:categoryService.buildCategoryTree()){boolean top=false;for(CategoryVO.ChildrenCategoryVO child:c.getChildren()){boolean sub=false;for(Question q:questions.stream().filter(x->x.getCategoryId().equals(child.getCategoryId())).toList()){if(!top){md.append("# ").append(c.getName()).append("\n");top=true;}if(!sub){md.append("## ").append(child.getName()).append("\n");sub=true;}md.append("### [").append(q.getTitle()).append("]").append("(https://notes.kamacoder.com/questions/").append(q.getQuestionId()).append(")\n");Note n=noteMap.get(q.getQuestionId());if(n!=null)md.append(n.getContent()).append("\n");}}}
        DownloadNoteVO vo=new DownloadNoteVO();vo.setMarkdown(md.toString());return ApiResponseUtil.success("生成笔记成功",vo);
    }

    @Override public ApiResponse<List<NoteRankListItem>> submitNoteRank(){return ApiResponseUtil.success("获取笔记排行榜成功",noteMapper.submitNoteRank());}
    @Override @NeedLogin public ApiResponse<List<NoteHeatMapItem>> submitNoteHeatMap(){return ApiResponseUtil.success("获取笔记热力图成功",noteMapper.submitNoteHeatMap(requestScopeData.getUserId()));}
    @Override @NeedLogin public ApiResponse<Top3Count> submitNoteTop3Count(){return ApiResponseUtil.success("获取笔记top3成功",noteMapper.submitNoteTop3Count(requestScopeData.getUserId()));}

    @Override
    public ApiResponse<List<NoteHotRankVO>> getHotRank(Integer limit){
        Set<ZSetOperations.TypedTuple<Object>> tuples=redisService.zReverseRangeWithScores(RedisKey.noteHotRank(),0,limit-1);
        if(CollectionUtils.isEmpty(tuples))return ApiResponseUtil.success("获取笔记热榜成功",Collections.emptyList());
        List<Integer> ids=tuples.stream().map(t->Integer.parseInt(String.valueOf(t.getValue()))).toList();List<Note> notes=noteMapper.findByIdBatch(ids);fillRealtimeCounts(notes);
        Map<Integer,Note> noteMap=notes.stream().collect(Collectors.toMap(Note::getNoteId,n->n));Map<Long,User> users=userService.getUserMapByIds(notes.stream().map(Note::getAuthorId).distinct().toList());Map<Integer,Question> qs=questionService.getQuestionMapByIds(notes.stream().map(Note::getQuestionId).distinct().toList());
        List<NoteHotRankVO> data=tuples.stream().map(t->{Note n=noteMap.get(Integer.parseInt(String.valueOf(t.getValue())));if(n==null)return null;NoteHotRankVO vo=new NoteHotRankVO();BeanUtils.copyProperties(n,vo);vo.setHotScore(t.getScore());vo.setDisplayContent(MarkdownUtil.extractIntroduction(n.getContent()));User u=users.get(n.getAuthorId());if(u!=null){NoteHotRankVO.SimpleAuthorVO a=new NoteHotRankVO.SimpleAuthorVO();BeanUtils.copyProperties(u,a);vo.setAuthor(a);}Question q=qs.get(n.getQuestionId());if(q!=null){NoteHotRankVO.SimpleQuestionVO qq=new NoteHotRankVO.SimpleQuestionVO();BeanUtils.copyProperties(q,qq);vo.setQuestion(qq);}return vo;}).filter(Objects::nonNull).toList();
        return ApiResponseUtil.success("获取笔记热榜成功",data);
    }

    @Override public void updateNoteHotScore(Integer noteId){noteHotRankService.updateNoteHotScore(noteId);evictHotRankCache();}
    @Override public void removeNoteFromHotRank(Integer noteId){noteHotRankService.removeNoteFromHotRank(noteId);evictHotRankCache();}
    @Override public void evictHotRankCache(){Set<String> keys=redisService.keys(RedisKey.noteHotRankListPattern());if(!CollectionUtils.isEmpty(keys))keys.forEach(redisService::delete);}

    private NoteVO buildBaseNoteVO(Note note){fillRealtimeCounts(note);return toNoteVO(note,userService.getUserMapByIds(Collections.singletonList(note.getAuthorId())),questionService.getQuestionMapByIds(Collections.singletonList(note.getQuestionId())),Collections.emptySet(),Collections.emptySet());}
    private NoteVO fillUserActions(NoteVO base){NoteVO vo=new NoteVO();BeanUtils.copyProperties(base,vo);vo.setAuthor(base.getAuthor());vo.setQuestion(base.getQuestion());NoteVO.UserActionsVO ua=new NoteVO.UserActionsVO();if(requestScopeData.isLogin()&&requestScopeData.getUserId()!=null){Long uid=requestScopeData.getUserId();ua.setIsLiked(noteLikeService.findUserLikedNoteIds(uid,Collections.singletonList(base.getNoteId())).contains(base.getNoteId()));ua.setIsCollected(collectionNoteService.findUserCollectedNoteIds(uid,Collections.singletonList(base.getNoteId())).contains(base.getNoteId()));}vo.setUserActions(ua);return vo;}
    private NoteVO toNoteVO(Note n,Map<Long,User> users,Map<Integer,Question> qs,Set<Integer> liked,Set<Integer> collected){NoteVO vo=new NoteVO();BeanUtils.copyProperties(n,vo);User u=users.get(n.getAuthorId());if(u!=null){NoteVO.SimpleAuthorVO a=new NoteVO.SimpleAuthorVO();BeanUtils.copyProperties(u,a);vo.setAuthor(a);}Question q=qs.get(n.getQuestionId());if(q!=null){NoteVO.SimpleQuestionVO qq=new NoteVO.SimpleQuestionVO();BeanUtils.copyProperties(q,qq);vo.setQuestion(qq);}NoteVO.UserActionsVO ua=new NoteVO.UserActionsVO();ua.setIsLiked(liked.contains(n.getNoteId()));ua.setIsCollected(collected.contains(n.getNoteId()));vo.setUserActions(ua);if(MarkdownUtil.needCollapsed(n.getContent())){vo.setNeedCollapsed(true);vo.setDisplayContent(MarkdownUtil.extractIntroduction(n.getContent()));}return vo;}
    private void fillRealtimeCounts(List<Note> notes){if(!CollectionUtils.isEmpty(notes))notes.forEach(this::fillRealtimeCounts);}    
    private void fillRealtimeCounts(Note n){if(n==null)return;Object like=redisService.get(LK+n.getNoteId()),collect=redisService.get(CK+n.getNoteId()),comment=redisService.get(MK+n.getNoteId());if(like!=null)n.setLikeCount(Integer.parseInt(String.valueOf(like)));if(collect!=null)n.setCollectCount(Integer.parseInt(String.valueOf(collect)));if(comment!=null)n.setCommentCount(Integer.parseInt(String.valueOf(comment)));}
    private void evictNoteDetailCache(Integer noteId){redisService.delete(RedisKey.noteDetail(noteId));noteLocalCache.invalidate("note:detail:"+noteId);}    
}
