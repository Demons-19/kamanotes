import { useEffect, useState } from 'react'
import {
  Message,
  MessageQueryParams,
  MessageType,
} from '@/domain/message/types.ts'
import { messageService } from '@/domain/message/service/messageService.ts'
import { Pagination } from '@/request/types'

const pageSize = 20

function buildQueryParams(activeTab: string, page: number): MessageQueryParams {
  if (activeTab === 'like') {
    return { type: String(MessageType.LIKE), page, pageSize }
  }
  if (activeTab === 'collect') {
    return { type: String(MessageType.COLLECT), page, pageSize }
  }
  if (activeTab === 'comment') {
    return { type: 'comment', page, pageSize }
  }
  if (activeTab === 'system') {
    return { type: 'system', page, pageSize }
  }
  return { page, pageSize }
}

export function useMessages(activeTab: string) {
  const [messages, setMessages] = useState<Message[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pagination, setPagination] = useState<Pagination>({
    page: 1,
    pageSize,
    total: 0,
  })

  useEffect(() => {
    setPage(1)
  }, [activeTab])

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true)
        const response = await messageService.getMessages(
          buildQueryParams(activeTab, page),
        )
        setMessages(response.data)
        setPagination(
          response.pagination ?? {
            page,
            pageSize,
            total: response.data.length,
          },
        )
      } catch (error) {
        console.log(error)
      } finally {
        setLoading(false)
      }
    }

    fetchData().then()
  }, [activeTab, page])

  async function markMessagesAsRead(messageIds: number[]) {
    setMessages((prev) =>
      prev.map((message) =>
        messageIds.includes(message.messageId)
          ? { ...message, isRead: true }
          : message,
      ),
    )
    await messageService.readMessages(messageIds)
  }

  async function deleteMessage(messageId: number) {
    setMessages((prev) =>
      prev.filter((message) => message.messageId !== messageId),
    )
    setPagination((prev) => ({ ...prev, total: Math.max(0, prev.total - 1) }))
    await messageService.deleteMessage(messageId)
  }

  async function markAllMessagesAsRead() {
    setMessages((prev) => prev.map((message) => ({ ...message, isRead: true })))
    await messageService.readAllMessages()
  }

  return {
    loading,
    messages,
    pagination,
    page,
    setPage,
    deleteMessage,
    markMessagesAsRead,
    markAllMessagesAsRead,
  }
}
