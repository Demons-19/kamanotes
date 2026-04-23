import { useEffect, useRef, useState } from 'react'
import {
  CommentQueryParams,
  Comment,
  CreateCommentRequest,
  ReplySort,
  NextCursor,
} from '@/domain/comment/types.ts'
import { commentService } from '@/domain/comment/service/commentService.ts'
import { useUser } from '@/domain/user/hooks/useUser.ts'

interface ReplyState {
  items: Comment[]
  page: number
  pageSize: number
  total: number
  loading: boolean
  loaded: boolean
  sort: ReplySort
}

const defaultReplyPageSize = 10

export function useComment(commentQueryParams: CommentQueryParams) {
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<NextCursor | null>(null)
  const [replyStates, setReplyStates] = useState<Record<number, ReplyState>>({})
  const requestIdRef = useRef(0)

  const user = useUser()

  async function loadComments(reset = false, cursor?: NextCursor) {
    const requestId = ++requestIdRef.current
    try {
      setLoading(true)
      const params: CommentQueryParams = {
        noteId: commentQueryParams.noteId,
        pageSize: commentQueryParams.pageSize ?? 10,
        sort: commentQueryParams.sort ?? 'hot',
      }
      if (cursor) {
        params.cursorCreatedAt = cursor.createdAt
        params.cursorCommentId = cursor.commentId
        params.cursorLikeCount = cursor.likeCount
        params.cursorReplyCount = cursor.replyCount
      }
      const res = await commentService.getCommentsService(params)
      if (requestId !== requestIdRef.current) {
        return
      }
      const data = res.data
      setComments((prev) => (reset ? data.items : [...prev, ...data.items]))
      setHasMore(data.hasMore)
      setNextCursor(data.nextCursor)
    } catch (error: unknown) {
      if (requestId !== requestIdRef.current) {
        return
      }
      if (error instanceof Error) {
        console.log(error.message)
      }
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    loadComments(true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    commentQueryParams.noteId,
    commentQueryParams.pageSize,
    commentQueryParams.sort,
  ])

  async function loadMoreComments() {
    if (!hasMore || loading || !nextCursor) {
      return
    }
    await loadComments(false, nextCursor)
  }

  async function loadReplies(
    rootCommentId: number,
    options?: {
      page?: number
      pageSize?: number
      sort?: ReplySort
      append?: boolean
    },
  ) {
    const currentState = replyStates[rootCommentId]
    const page = options?.page ?? currentState?.page ?? 1
    const pageSize =
      options?.pageSize ?? currentState?.pageSize ?? defaultReplyPageSize
    const sort = options?.sort ?? currentState?.sort ?? 'asc'
    const append = options?.append ?? false

    setReplyStates((prev) => ({
      ...prev,
      [rootCommentId]: {
        items: prev[rootCommentId]?.items ?? [],
        page,
        pageSize,
        total: prev[rootCommentId]?.total ?? 0,
        loading: true,
        loaded: prev[rootCommentId]?.loaded ?? false,
        sort,
      },
    }))

    try {
      const res = await commentService.getRepliesService({
        rootCommentId,
        page,
        pageSize,
        sort,
      })

      setReplyStates((prev) => ({
        ...prev,
        [rootCommentId]: {
          items: append
            ? [...(prev[rootCommentId]?.items ?? []), ...res.data]
            : res.data,
          page,
          pageSize,
          total: res.pagination?.total ?? res.data.length,
          loading: false,
          loaded: true,
          sort,
        },
      }))
    } catch (error: unknown) {
      setReplyStates((prev) => ({
        ...prev,
        [rootCommentId]: {
          items: prev[rootCommentId]?.items ?? [],
          page,
          pageSize,
          total: prev[rootCommentId]?.total ?? 0,
          loading: false,
          loaded: prev[rootCommentId]?.loaded ?? false,
          sort,
        },
      }))
      if (error instanceof Error) {
        console.log(error.message)
      }
    }
  }

  async function loadMoreReplies(rootCommentId: number) {
    const currentState = replyStates[rootCommentId]
    if (!currentState || currentState.loading) {
      return
    }
    const nextPage = currentState.page + 1
    await loadReplies(rootCommentId, {
      page: nextPage,
      pageSize: currentState.pageSize,
      sort: currentState.sort,
      append: true,
    })
  }

  async function changeReplySort(rootCommentId: number, sort: ReplySort) {
    await loadReplies(rootCommentId, {
      page: 1,
      pageSize: replyStates[rootCommentId]?.pageSize ?? defaultReplyPageSize,
      sort,
      append: false,
    })
  }

  function insertReplyToTopLevel(
    tree: Comment[],
    parentId: number,
    reply: Comment,
  ): Comment[] {
    return tree.map((comment) => {
      if (comment.commentId === parentId) {
        return {
          ...comment,
          replyCount: (comment.replyCount || 0) + 1,
          replies: comment.replies ? [...comment.replies, reply] : [reply],
        }
      }
      return comment
    })
  }

  async function createComment(params: CreateCommentRequest) {
    const newComment: Comment = {
      commentId: -1,
      rootCommentId: params.parentId,
      noteId: params.noteId,
      content: params.content,
      likeCount: 0,
      replyCount: 0,
      createdAt: new Date().toISOString(),
      author: {
        userId: user.userId,
        username: user.username,
        avatarUrl: user.avatarUrl,
      },
      replyToUser: params.replyToUserId
        ? {
            userId: String(params.replyToUserId),
            username: '',
            avatarUrl: '',
          }
        : undefined,
      userActions: {
        isLiked: false,
      },
      replies: [],
    }

    setComments((prev) => {
      if (params.parentId !== undefined && params.parentId !== 0) {
        return insertReplyToTopLevel(prev, params.parentId, newComment)
      }
      if ((commentQueryParams.sort ?? 'hot') === 'latest') {
        return [newComment, ...prev]
      }
      return prev
    })

    const resp = await commentService.createCommentService(params)
    const commentId = resp.data

    setComments((prev) =>
      prev.map((comment) => {
        if (comment.commentId === -1) {
          return { ...comment, commentId, rootCommentId: commentId }
        }
        if (comment.commentId === params.parentId) {
          return {
            ...comment,
            replies: comment.replies?.map((reply) =>
              reply.commentId === -1
                ? { ...reply, commentId, rootCommentId: params.parentId }
                : reply,
            ),
          }
        }
        return comment
      }),
    )

    if (params.parentId) {
      setReplyStates((prev) => {
        const state = prev[params.parentId]
        if (!state) return prev
        return {
          ...prev,
          [params.parentId]: {
            ...state,
            items: [
              ...state.items,
              { ...newComment, commentId, rootCommentId: params.parentId },
            ],
            total: state.total + 1,
          },
        }
      })
    }
  }

  function updateLikeInList(
    list: Comment[],
    commentId: number,
    isLiked: boolean,
  ) {
    return list.map((comment) =>
      comment.commentId === commentId
        ? {
            ...comment,
            likeCount: comment.likeCount + (isLiked ? 1 : -1),
            userActions: { ...comment.userActions, isLiked },
          }
        : comment,
    )
  }

  async function likeComment(commentId: number) {
    const currentTop = comments.find(
      (comment) => comment.commentId === commentId,
    )
    const currentReplyEntry = Object.entries(replyStates).find(([, state]) =>
      state.items.some((item) => item.commentId === commentId),
    )
    const target =
      currentTop ||
      currentReplyEntry?.[1].items.find((item) => item.commentId === commentId)
    if (!target) return

    const nextIsLiked = !target.userActions?.isLiked
    setComments((prev) => updateLikeInList(prev, commentId, nextIsLiked))
    if (currentReplyEntry) {
      const rootCommentId = Number(currentReplyEntry[0])
      setReplyStates((prev) => ({
        ...prev,
        [rootCommentId]: {
          ...prev[rootCommentId],
          items: updateLikeInList(
            prev[rootCommentId].items,
            commentId,
            nextIsLiked,
          ),
        },
      }))
    }

    if (nextIsLiked) {
      await commentService.likeCommentService(commentId)
    } else {
      await commentService.unlikeCommentService(commentId)
    }
  }

  return {
    loading,
    comments,
    hasMore,
    replyStates,
    createComment,
    likeComment,
    loadReplies,
    loadMoreReplies,
    changeReplySort,
    loadMoreComments,
  }
}
