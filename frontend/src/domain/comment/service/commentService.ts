import {
  CommentQueryParams,
  Comment,
  CreateCommentRequest,
  ReplyQueryParams,
} from '@/domain/comment/types.ts'
import { commentApiList } from '@/domain/comment/api/commentApi.ts'
import { httpClient } from '@/request'

export const commentService = {
  getCommentsService: (params: CommentQueryParams) => {
    return httpClient.request<Comment[]>(commentApiList.comments, {
      queryParams: params,
    })
  },

  getRepliesService: (params: ReplyQueryParams) => {
    const { rootCommentId, ...queryParams } = params
    return httpClient.request<Comment[]>(commentApiList.commentReplies, {
      pathParams: [rootCommentId],
      queryParams,
    })
  },

  createCommentService: (request: CreateCommentRequest) => {
    return httpClient.request<number>(commentApiList.createComment, {
      body: request,
    })
  },

  likeCommentService: (commentId: number) => {
    return httpClient.request(commentApiList.likeComment, {
      pathParams: [commentId],
    })
  },

  unlikeCommentService: (commentId: number) => {
    return httpClient.request(commentApiList.unlikeComment, {
      pathParams: [commentId],
    })
  },
}
