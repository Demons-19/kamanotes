import React from 'react'
import { Badge, Tooltip } from 'antd'
import {
  MessageOutlined,
  HeartOutlined,
  BellOutlined,
  StarOutlined,
} from '@ant-design/icons'

interface MessageStatsProps {
  totalMessages: number
  unreadCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  systemCount: number
  unreadLikeCount: number
  unreadCommentCount: number
  unreadCollectCount: number
  unreadSystemCount: number
}

const MessageStats: React.FC<MessageStatsProps> = ({
  totalMessages,
  unreadCount,
  likeCount,
  commentCount,
  collectCount,
  systemCount,
  unreadLikeCount,
  unreadCommentCount,
  unreadCollectCount,
  unreadSystemCount,
}) => {
  const stats = [
    {
      label: '点赞',
      icon: <HeartOutlined style={{ color: '#ff4d4f' }} />,
      count: likeCount,
      unread: unreadLikeCount,
    },
    {
      label: '评论',
      icon: <MessageOutlined style={{ color: '#1890ff' }} />,
      count: commentCount,
      unread: unreadCommentCount,
    },
    {
      label: '收藏',
      icon: <StarOutlined style={{ color: '#722ed1' }} />,
      count: collectCount,
      unread: unreadCollectCount,
    },
    {
      label: '系统',
      icon: <BellOutlined style={{ color: '#52c41a' }} />,
      count: systemCount,
      unread: unreadSystemCount,
    },
  ]

  return (
    <div className="flex flex-col items-start gap-2 text-sm text-gray-600 sm:flex-row sm:items-center sm:gap-4">
      <div className="flex items-center gap-2">
        <span>共 {totalMessages} 条消息</span>
        <span className="font-medium text-blue-600">{unreadCount} 条未读</span>
      </div>

      <div className="flex items-center gap-3">
        {stats.map((stat) => (
          <Tooltip
            key={stat.label}
            title={`${stat.label}消息: ${stat.count}条 (${stat.unread}条未读)`}
          >
            <div className="flex cursor-pointer items-center gap-1 transition-colors hover:text-gray-800">
              {stat.icon}
              <span className="hidden sm:inline">{stat.count}</span>
              {stat.unread > 0 && <Badge count={stat.unread} size="small" />}
            </div>
          </Tooltip>
        ))}
      </div>
    </div>
  )
}

export default MessageStats
