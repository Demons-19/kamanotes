import { httpClient } from '@/request'
import { messageApi } from '@/domain/message/api/messageApi.ts'
import { Message, MessageQueryParams } from '@/domain/message/types.ts'
import { Pagination } from '@/request/types'

export const messageService = {
  getMessages: (queryParams: MessageQueryParams = {}) => {
    return httpClient.request<Message[]>(messageApi.messages, {
      queryParams,
    })
  },

  getUnreadCount: () => {
    return httpClient.request<number>(messageApi.unreadCount, {})
  },

  readMessages: (messageIds: number[]) => {
    return httpClient.request<null>(messageApi.readMessageBatch, {
      body: {
        messageIds,
      },
    })
  },

  readAllMessages: () => {
    return httpClient.request<null>(messageApi.readAll, {})
  },

  deleteMessage: (messageId: number) => {
    return httpClient.request<null>(messageApi.deleteMessage, {
      pathParams: [messageId],
    })
  },

  publishAnnouncement: (content: string) => {
    return httpClient.request<null>(messageApi.publishAnnouncement, {
      body: { content },
    })
  },
}

export type MessageListResponse = {
  data: Message[]
  pagination?: Pagination
}
