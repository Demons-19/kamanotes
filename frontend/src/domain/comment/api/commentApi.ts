import { ApiList } from '@/request'

export const commentApiList: ApiList = {
  comments: ['GET', '/api/comments'],
  commentReplies: ['GET', '/api/comments/{rootCommentId}/replies'],
  createComment: ['POST', '/api/comments'],
  likeComment: ['POST', '/api/comments/{commentId}/like'],
  unlikeComment: ['DELETE', '/api/comments/{commentId}/like'],
}
