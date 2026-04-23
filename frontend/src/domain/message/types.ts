import { QuestionSummary } from '@/domain/question'

export enum MessageType {
  LIKE = 1,
  COMMENT = 2,
  SYSTEM = 3,
  COLLECT = 4,
  REPLY = 5,
  ANNOUNCEMENT = 6,
}

export enum TargetType {
  NOTE = 1,
  COMMENT = 2,
}

export interface MessageQueryParams {
  type?: MessageType | string
  isRead?: boolean
  page?: number
  pageSize?: number
}

export interface Message {
  messageId: number
  sender: {
    userId: string
    username: string
    avatarUrl?: string
  }
  type: MessageType
  target?: {
    targetId: number
    targetType: TargetType
    questionSummary?: QuestionSummary
  }
  isRead: boolean
  content: string
  createdAt: string
}
