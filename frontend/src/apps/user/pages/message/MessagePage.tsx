import React, { useState, useMemo } from 'react'
import {
  Tabs,
  Badge,
  Button,
  Avatar,
  Dropdown,
  List,
  Empty,
  Spin,
  message,
  Modal,
  Input,
} from 'antd'
import {
  MessageOutlined,
  HeartOutlined,
  BellOutlined,
  CheckOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  MoreOutlined,
  UserOutlined,
  BookOutlined,
  NotificationOutlined,
} from '@ant-design/icons'
import { useMessages } from '@/domain/message/hooks/useMessages.ts'
import TimeAgo from '@/base/components/timeAgo/TimeAgo.tsx'
import MessageStats from './components/MessageStats.tsx'
import Panel from '@/base/components/panel/Panel'
import { messageService } from '@/domain/message/service/messageService.ts'
import { useUser } from '@/domain/user/hooks/useUser.ts'
import { Admin } from '@/domain/user/types/types.ts'
import { MessageType } from '@/domain/message/types.ts'
import './MessagePage.css'

const messageTypeConfig = {
  [MessageType.LIKE]: {
    label: '点赞消息',
    icon: HeartOutlined,
    color: '#ff4d4f',
    bgColor: '#fff2f0',
    borderColor: '#ffccc7',
  },
  [MessageType.COMMENT]: {
    label: '评论消息',
    icon: MessageOutlined,
    color: '#1890ff',
    bgColor: '#f0f9ff',
    borderColor: '#91d5ff',
  },
  [MessageType.SYSTEM]: {
    label: '系统消息',
    icon: BellOutlined,
    color: '#52c41a',
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f',
  },
  [MessageType.COLLECT]: {
    label: '收藏消息',
    icon: BellOutlined,
    color: '#722ed1',
    bgColor: '#f9f0ff',
    borderColor: '#d3adf7',
  },
  [MessageType.REPLY]: {
    label: '回复消息',
    icon: MessageOutlined,
    color: '#1677ff',
    bgColor: '#f0f5ff',
    borderColor: '#adc6ff',
  },
  [MessageType.ANNOUNCEMENT]: {
    label: '系统公告',
    icon: NotificationOutlined,
    color: '#fa8c16',
    bgColor: '#fff7e6',
    borderColor: '#ffd591',
  },
}

const MessagePage: React.FC = () => {
  const {
    messages,
    deleteMessage,
    markMessagesAsRead,
    markAllMessagesAsRead,
    loading,
  } = useMessages()
  const user = useUser()
  const isAdmin = user.isAdmin === Admin.ADMIN

  const [activeTab, setActiveTab] = useState<string>('all')
  const [announcementOpen, setAnnouncementOpen] = useState(false)
  const [announcementContent, setAnnouncementContent] = useState('')
  const [publishing, setPublishing] = useState(false)

  const groupedMessages = useMemo(() => {
    const grouped = {
      all: messages,
      like: messages.filter((msg) => msg.type === MessageType.LIKE),
      comment: messages.filter(
        (msg) =>
          msg.type === MessageType.COMMENT || msg.type === MessageType.REPLY,
      ),
      system: messages.filter(
        (msg) =>
          msg.type === MessageType.SYSTEM ||
          msg.type === MessageType.ANNOUNCEMENT,
      ),
      collect: messages.filter((msg) => msg.type === MessageType.COLLECT),
    }
    return grouped
  }, [messages])

  const getUnreadCount = (messageList: typeof messages) => {
    return messageList.filter((msg) => !msg.isRead).length
  }

  const handleMessageClick = (message: any) => {
    if (!message.isRead) {
      markMessagesAsRead([message.messageId])
    }
    if (message.target) {
      console.log('Navigate to:', message.target)
    }
  }

  const handleDeleteMessage = (messageId: number) => {
    deleteMessage(messageId)
    message.success('消息已删除')
  }

  const handleMarkAsRead = (messageId: number) => {
    markMessagesAsRead([messageId])
    message.success('已标记为已读')
  }

  const handleMarkAllAsRead = () => {
    markAllMessagesAsRead()
    message.success('已全部标记为已读')
  }

  const handlePublishAnnouncement = async () => {
    if (!announcementContent.trim()) {
      message.warning('请输入公告内容')
      return
    }
    try {
      setPublishing(true)
      await messageService.publishAnnouncement(announcementContent.trim())
      message.success('系统公告已发布')
      setAnnouncementOpen(false)
      setAnnouncementContent('')
    } catch (error: any) {
      console.error(error)
      message.error(error.message || '发布失败，请确认你是管理员')
    } finally {
      setPublishing(false)
    }
  }

  const getActionText = (type: number) => {
    switch (type) {
      case MessageType.LIKE:
        return '赞了你的内容'
      case MessageType.COMMENT:
        return '评论了你的笔记'
      case MessageType.REPLY:
        return '回复了你的评论'
      case MessageType.SYSTEM:
      case MessageType.ANNOUNCEMENT:
        return ''
      case MessageType.COLLECT:
        return '收藏了你的笔记'
      default:
        return ''
    }
  }

  const renderMessageList = (messageList: typeof messages) => {
    if (loading) {
      return (
        <div className="flex items-center justify-center py-12">
          <Spin size="large" />
        </div>
      )
    }

    if (messageList.length === 0) {
      return (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <div className="text-center">
              <p className="text-lg font-medium text-gray-900">暂无消息</p>
              <p className="text-sm text-gray-500">当有新消息时会在这里显示</p>
            </div>
          }
        />
      )
    }

    return (
      <List
        dataSource={messageList}
        renderItem={(item, idx) => renderMessageItem(item, idx)}
        className="rounded-lg bg-white"
        itemLayout="horizontal"
        split={false}
      />
    )
  }

  const renderMessageItem = (messageItem: any, idx: number) => {
    const config = messageTypeConfig[messageItem.type as MessageType] || {
      label: '未知消息',
      icon: BellOutlined,
      color: '#999',
      bgColor: '#f5f5f5',
      borderColor: '#d9d9d9',
    }
    const actionText = getActionText(messageItem.type)
    const isSystem =
      messageItem.type === MessageType.SYSTEM ||
      messageItem.type === MessageType.ANNOUNCEMENT
    const questionSummary = messageItem.target?.questionSummary

    const baseDelay = 80
    const baseDuration = 0.5
    const durationStep = 0.06
    const maxDelay = 800
    const maxDuration = 0.9
    const delay = Math.min(idx * baseDelay, maxDelay)
    const duration = Math.min(baseDuration + idx * durationStep, maxDuration)

    return (
      <List.Item
        key={messageItem.messageId}
        className={`message-item group mb-4 flex rounded-xl border-0 px-4 py-3 shadow-sm ${
          !messageItem.isRead ? 'unread' : ''
        } message-item-animate`}
        style={{
          background: config.bgColor,
          borderLeft: !messageItem.isRead
            ? `4px solid ${config.borderColor}`
            : '4px solid transparent',
          animationDelay: `${delay}ms`,
          animationDuration: `${duration}s`,
        }}
        onClick={() => handleMessageClick(messageItem)}
      >
        <Avatar
          src={messageItem.sender.avatarUrl}
          icon={<UserOutlined />}
          size={44}
          className="message-avatar mr-4 flex-shrink-0"
        >
          {(messageItem.sender.username || '系').charAt(0).toUpperCase()}
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="mb-1 flex flex-wrap items-center gap-2">
            <span className="text-base font-semibold text-gray-900">
              {isSystem ? '系统通知' : messageItem.sender.username}
            </span>
            {!isSystem && <span className="text-gray-700">{actionText}</span>}
            {!messageItem.isRead && (
              <span className="ml-2 rounded bg-red-500 px-2 py-0.5 text-xs text-white">
                新
              </span>
            )}
          </div>

          {questionSummary && (
            <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-white/75 px-3 py-1 text-sm text-emerald-700 shadow-sm">
              <BookOutlined />
              <span className="font-medium">关联题目：</span>
              <span className="truncate">{questionSummary.title}</span>
            </div>
          )}

          <div className="message-content mb-2 text-[15px] leading-relaxed text-gray-800">
            {messageItem.content}
          </div>
          <div className="message-time flex items-center gap-2 text-xs text-gray-500">
            <TimeAgo datetime={messageItem.createdAt} />
          </div>
        </div>
        <Dropdown
          menu={{
            items: [
              ...(!messageItem.isRead
                ? [
                    {
                      key: 'read',
                      icon: <CheckOutlined />,
                      label: '标记已读',
                      onClick: () => handleMarkAsRead(messageItem.messageId),
                    },
                  ]
                : []),
              {
                key: 'delete',
                icon: <DeleteOutlined />,
                label: '删除消息',
                danger: true,
                onClick: () => handleDeleteMessage(messageItem.messageId),
              },
            ],
          }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Button
            type="text"
            icon={<MoreOutlined />}
            size="small"
            onClick={(e) => e.stopPropagation()}
            className="message-action-btn opacity-0 transition-opacity duration-200 group-hover:opacity-100"
          />
        </Dropdown>
      </List.Item>
    )
  }

  const tabItems = [
    {
      key: 'all',
      label: (
        <span className="flex items-center gap-1">
          全部
          {getUnreadCount(groupedMessages.all) > 0 && (
            <Badge count={getUnreadCount(groupedMessages.all)} size="small" />
          )}
        </span>
      ),
      children: renderMessageList(groupedMessages.all),
    },
    {
      key: 'like',
      label: (
        <span className="flex items-center gap-1">
          点赞
          {getUnreadCount(groupedMessages.like) > 0 && (
            <Badge count={getUnreadCount(groupedMessages.like)} size="small" />
          )}
        </span>
      ),
      children: renderMessageList(groupedMessages.like),
    },
    {
      key: 'comment',
      label: (
        <span className="flex items-center gap-1">
          评论
          {getUnreadCount(groupedMessages.comment) > 0 && (
            <Badge
              count={getUnreadCount(groupedMessages.comment)}
              size="small"
            />
          )}
        </span>
      ),
      children: renderMessageList(groupedMessages.comment),
    },
    {
      key: 'collect',
      label: (
        <span className="flex items-center gap-1">
          收藏
          {getUnreadCount(groupedMessages.collect) > 0 && (
            <Badge
              count={getUnreadCount(groupedMessages.collect)}
              size="small"
            />
          )}
        </span>
      ),
      children: renderMessageList(groupedMessages.collect),
    },
    {
      key: 'system',
      label: (
        <span className="flex items-center gap-1">
          系统
          {getUnreadCount(groupedMessages.system) > 0 && (
            <Badge
              count={getUnreadCount(groupedMessages.system)}
              size="small"
            />
          )}
        </span>
      ),
      children: renderMessageList(groupedMessages.system),
    },
  ]

  return (
    <>
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-emerald-50 p-6">
        <div className="mx-auto max-w-5xl">
          <Panel>
            <div className="mb-6 flex items-center justify-between">
              <div>
                <h1 className="mb-2 text-3xl font-bold text-gray-900">
                  消息中心
                </h1>
                <p className="text-gray-500">查看点赞、评论、收藏和系统通知</p>
              </div>
              <div className="flex items-center gap-3">
                {isAdmin && (
                  <Button
                    type="primary"
                    icon={<NotificationOutlined />}
                    onClick={() => setAnnouncementOpen(true)}
                  >
                    发布公告
                  </Button>
                )}
                <Button
                  icon={<CheckCircleOutlined />}
                  onClick={handleMarkAllAsRead}
                  disabled={getUnreadCount(groupedMessages.all) === 0}
                >
                  全部已读
                </Button>
              </div>
            </div>

            <MessageStats
              totalMessages={messages.length}
              unreadCount={messages.filter((item) => !item.isRead).length}
              likeCount={groupedMessages.like.length}
              commentCount={groupedMessages.comment.length}
              collectCount={groupedMessages.collect.length}
              systemCount={groupedMessages.system.length}
              unreadLikeCount={getUnreadCount(groupedMessages.like)}
              unreadCommentCount={getUnreadCount(groupedMessages.comment)}
              unreadCollectCount={getUnreadCount(groupedMessages.collect)}
              unreadSystemCount={getUnreadCount(groupedMessages.system)}
            />

            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              items={tabItems}
              className="message-tabs"
            />
          </Panel>
        </div>
      </div>

      <Modal
        title="发布系统公告"
        open={announcementOpen}
        onOk={handlePublishAnnouncement}
        onCancel={() => {
          setAnnouncementOpen(false)
          setAnnouncementContent('')
        }}
        okText="发布"
        cancelText="取消"
        confirmLoading={publishing}
      >
        <Input.TextArea
          value={announcementContent}
          onChange={(e) => setAnnouncementContent(e.target.value)}
          placeholder="请输入系统公告内容"
          autoSize={{ minRows: 4, maxRows: 8 }}
          maxLength={500}
          showCount
        />
      </Modal>
    </>
  )
}

export default MessagePage
