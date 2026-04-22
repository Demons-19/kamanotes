import { httpClient } from '@/request'
import { noteApiList } from '../api/noteApi.ts'
import {
  CreateNoteParams,
  DownloadNote,
  NoteHeatMapItem,
  NoteHotRankItem,
  NoteQueryParams,
  NoteRankListItem,
  NoteTop3Count,
  NoteWithRelations,
} from '../types/serviceTypes.ts'

export const noteService = {
  /**
   * 获取笔记列表服务
   */
  getNoteList: (params: NoteQueryParams) => {
    return httpClient.request<NoteWithRelations[]>(noteApiList.getNoteList, {
      queryParams: params,
    })
  },

  /**
   * 获取笔记详情服务
   */
  getNoteDetailService: (noteId: number) => {
    return httpClient.request<NoteWithRelations>(noteApiList.getNoteDetail, {
      pathParams: [noteId],
    })
  },

  /**
   * 创建笔记服务
   */
  createNoteService: (params: CreateNoteParams) => {
    return httpClient.request<{ noteId: number }>(noteApiList.createNote, {
      body: params,
    })
  },

  /**
   * 删除笔记服务
   */
  deleteNoteService: (noteId: number) => {
    return httpClient.request<{ noteId: number }>(noteApiList.deleteNote, {
      pathParams: [noteId],
    })
  },

  /**
   * 更新笔记服务
   */
  updateNoteService: (noteId: number, params: CreateNoteParams) => {
    return httpClient.request<{ noteId: number }>(noteApiList.updateNote, {
      pathParams: [noteId],
      body: params,
    })
  },

  /**
   * 获取笔记排行榜服务
   */
  getNoteRankListService: () => {
    return httpClient.request<NoteRankListItem[]>(noteApiList.getNoteRankList)
  },

  /**
   * 获取笔记热力图服务
   */
  getHeatMapService: () => {
    return httpClient.request<NoteHeatMapItem[]>(noteApiList.getHeatMap)
  },

  /**
   * 获取 top3Count
   */
  getTop3CountService: () => {
    return httpClient.request<NoteTop3Count>(noteApiList.getTop3Count)
  },

  /**
   * 获取热度排行榜
   */
  getHotRankService: (limit?: number) => {
    return httpClient.request<NoteHotRankItem[]>(noteApiList.getHotRank, {
      queryParams: limit ? { limit } : undefined,
    })
  },

  /**
   * 下载笔记
   */
  downloadNoteService: () => {
    return httpClient.request<DownloadNote>(noteApiList.downloadNote)
  },
}
