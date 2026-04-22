import React, { useState } from 'react'
import { Avatar, Button, Pagination, Segmented } from 'antd'
import { LikeOutlined, LikeFilled, MessageOutlined } from '@ant-design/icons'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import CommentInput from './CommentInput.tsx'
import { Comment, CommentSort } from '@/domain/comment/types.ts'
import { useComment } from '@/domain/comment/hooks/useComment.ts'
import './CommentList.css'

interface CommentListProps {
  noteId: number
  onCommentCountChange?: () => void
}

const CommentList: React.FC<CommentListProps> = ({ noteId }) => {
  const [commentQueryParams, setCommentQueryParams] = useState({
    noteId,
    page: 1,
    pageSize: 10,
    sort: 'hot' as CommentSort,
  })

  const {
    comments,
    loading,
    createComment,
    likeComment,
    pagination,
    replyStates,
    loadReplies,
    loadMoreReplies,
    changeReplySort,
  } = useComment(commentQueryParams)

  const [replyTo, setReplyTo] = useState<Comment | null>(null)
  const [showReplies, setShowReplies] = useState<Set<number>>(new Set())

  const handleReply = (comment: Comment) => {
    setReplyTo(comment)
  }

  const toggleReplies = async (comment: Comment) => {
    const commentId = comment.commentId
    const newShowReplies = new Set(showReplies)
    if (newShowReplies.has(commentId)) {
      newShowReplies.delete(commentId)
      setShowReplies(newShowReplies)
      return
    }

    newShowReplies.add(commentId)
    setShowReplies(newShowReplies)

    const replyState = replyStates[commentId]
    if (!replyState?.loaded) {
      await loadReplies(commentId, { page: 1, pageSize: 10, sort: 'asc' })
    }
  }

  const renderReplyItem = (reply: Comment) => {
    return (
      <div
        key={reply.commentId}
        className="comment-reply rounded-lg p-2 transition-all duration-200 hover:bg-gray-50"
      >
        <div className="flex items-start gap-3">
          <Avatar
            size="small"
            src={reply.author?.avatarUrl}
            className="comment-avatar mt-1 flex-shrink-0"
          />
          <div className="min-w-0 flex-1">
            <div className="mb-1 flex items-center gap-2">
              <span className="comment-username text-sm text-gray-900">
                {reply.author?.username}
              </span>
              {reply.replyToUser?.username && (
                <>
                  <span className="text-sm text-blue-500">回复</span>
                  <span className="text-sm font-medium text-blue-500">
                    @{reply.replyToUser.username}
                  </span>
                </>
              )}
            </div>
            <div className="comment-content mb-2 text-sm text-gray-700">
              {reply.content}
            </div>
            <div className="comment-actions flex items-center gap-4 text-xs text-gray-500">
              <span className="comment-time">
                {formatDistanceToNow(new Date(reply.createdAt), {
                  addSuffix: true,
                  locale: zhCN,
                })}
              </span>
              <Button
                type="text"
                size="small"
                icon={
                  reply.userActions?.isLiked ? (
                    <LikeFilled className="text-red-500" />
                  ) : (
                    <LikeOutlined />
                  )
                }
                onClick={() => likeComment(reply.commentId)}
                className="comment-action-btn comment-like-btn h-auto p-0 text-xs"
              >
                {reply.likeCount || 0}
              </Button>
              <Button
                type="text"
                size="small"
                onClick={() => handleReply(reply)}
                className="comment-action-btn comment-reply-btn h-auto p-0 text-xs"
              >
                回复
              </Button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  const renderMainComment = (comment: Comment) => {
    const previewReplies = comment.replies || []
    const replyState = replyStates[comment.commentId]
    const effectiveReplyCount = replyState?.total ?? comment.replyCount ?? 0
    const hasReplies = effectiveReplyCount > 0
    const isRepliesVisible = showReplies.has(comment.commentId)
    const loadedReplies = replyState?.items || []
    const displayedReplies = isRepliesVisible ? loadedReplies : previewReplies
    const loadedCount = loadedReplies.length
    const hasMoreReplies = effectiveReplyCount > loadedCount

    return (
      <div
        key={comment.commentId}
        className="comment-item rounded-lg border-b border-gray-100 p-3 transition-all duration-200 last:border-b-0 hover:bg-gray-50"
      >
        <div className="flex items-start gap-3">
          <Avatar
            src={comment.author?.avatarUrl}
            className="comment-avatar flex-shrink-0"
          />
          <div className="min-w-0 flex-1">
            <div className="mb-2 flex items-center gap-2">
              <span className="comment-username font-medium text-gray-900">
                {comment.author?.username}
              </span>
              <span className="comment-time text-xs text-gray-500">
                {formatDistanceToNow(new Date(comment.createdAt), {
                  addSuffix: true,
                  locale: zhCN,
                })}
              </span>
            </div>
            <div className="comment-content mb-3 text-gray-700">
              {comment.content}
            </div>
            <div className="comment-actions flex items-center gap-4">
              <Button
                type="text"
                icon={
                  comment.userActions?.isLiked ? (
                    <LikeFilled className="text-red-500" />
                  ) : (
                    <LikeOutlined />
                  )
                }
                onClick={() => likeComment(comment.commentId)}
                className="comment-action-btn comment-like-btn flex items-center gap-1"
              >
                {comment.likeCount || 0}
              </Button>
              <Button
                type="text"
                icon={<MessageOutlined />}
                onClick={() => handleReply(comment)}
                className="comment-action-btn comment-reply-btn flex items-center gap-1"
              >
                回复
              </Button>
              {hasReplies && (
                <Button
                  type="text"
                  onClick={() => toggleReplies(comment)}
                  className="text-blue-500 hover:text-blue-600"
                >
                  {isRepliesVisible
                    ? '收起回复'
                    : `查看全部 ${effectiveReplyCount} 条回复`}
                </Button>
              )}
            </div>

            {!isRepliesVisible && previewReplies.length > 0 && (
              <div className="mt-4 space-y-2 rounded-lg bg-gray-50 p-3">
                <div className="text-xs text-gray-500">精选回复预览</div>
                {previewReplies.map(renderReplyItem)}
              </div>
            )}

            {isRepliesVisible && (
              <div className="mt-4 rounded-lg bg-gray-50 p-3">
                <div className="mb-3 flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-800">
                    全部回复 {effectiveReplyCount} 条
                  </span>
                  <Segmented
                    size="small"
                    value={replyState?.sort || 'asc'}
                    options={[
                      { label: '最早', value: 'asc' },
                      { label: '最新', value: 'desc' },
                    ]}
                    onChange={(value) =>
                      changeReplySort(
                        comment.commentId,
                        value as 'asc' | 'desc',
                      )
                    }
                  />
                </div>

                <div className="space-y-2">
                  {(displayedReplies || []).map(renderReplyItem)}
                </div>

                {replyState?.loading && (
                  <div className="py-3 text-center text-sm text-gray-500">
                    加载回复中...
                  </div>
                )}

                {hasMoreReplies && !replyState?.loading && (
                  <div className="pt-3 text-center">
                    <Button
                      type="link"
                      onClick={() => loadMoreReplies(comment.commentId)}
                    >
                      查看更多回复
                    </Button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="comment-list space-y-8">
      <div className="mb-4">
        <h3 className="mb-2 text-lg font-medium text-gray-900">发表评论</h3>
        <CommentInput
          noteId={noteId}
          parentId={replyTo?.rootCommentId || replyTo?.commentId}
          replyTo={replyTo}
          onComment={async (params) => {
            await createComment(params)
            setReplyTo(null)
          }}
          onCancel={() => setReplyTo(null)}
        />
      </div>

      <div>
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-medium text-gray-900">
            评论 ({pagination?.total ?? comments?.length ?? 0})
          </h3>
          <Segmented
            value={commentQueryParams.sort}
            options={[
              { label: '最热', value: 'hot' },
              { label: '最新', value: 'latest' },
            ]}
            onChange={(value) => {
              setCommentQueryParams((prev) => ({
                ...prev,
                page: 1,
                sort: value as CommentSort,
              }))
            }}
          />
        </div>
        {loading ? (
          <div className="comment-loading">加载中...</div>
        ) : comments && comments.length > 0 ? (
          <div className="space-y-1">{comments.map(renderMainComment)}</div>
        ) : (
          <div className="comment-empty">
            <div className="mb-4 text-4xl">💬</div>
            <div>暂无评论，快来发表第一条评论吧！</div>
          </div>
        )}
      </div>

      <div className="mt-4 flex justify-center">
        <Pagination
          total={pagination?.total}
          current={commentQueryParams.page}
          pageSize={commentQueryParams.pageSize}
          onChange={(page, pageSize) => {
            setCommentQueryParams((prev) => ({
              ...prev,
              page,
              pageSize,
            }))
          }}
          showSizeChanger={false}
          showTotal={(total) => `共 ${total} 条一级评论`}
        />
      </div>
    </div>
  )
}

export default CommentList
