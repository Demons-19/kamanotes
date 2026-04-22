import React, { useEffect, useState } from 'react'
import { Avatar } from 'antd'
import { UserOutlined, FireOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { NoteHotRankItem } from '../types/serviceTypes.ts'
import { BronzeTrophy, GoldTrophy, SliverTrophy } from '../../../base/icon'
import { noteService } from '../service/noteService.ts'
import { USER_HOME, QUESTION, NOTE_DETAIL } from '@/apps/user/router/config.ts'

interface NoteHotRankListProps {
  limit?: number
  showEmpty?: boolean
}

const NoteHotRankList: React.FC<NoteHotRankListProps> = ({
  limit = 10,
  showEmpty = true,
}) => {
  const [rankList, setRankList] = useState<NoteHotRankItem[]>([])

  useEffect(() => {
    const fetchData = async () => {
      const { data } = await noteService.getHotRankService(limit)
      setRankList(data)
    }
    fetchData().then()
  }, [limit])

  const rankMap = (rank: number) => {
    switch (rank) {
      case 1:
        return (
          <div className="flex justify-center">
            <GoldTrophy />
          </div>
        )
      case 2:
        return (
          <div className="flex justify-center">
            <SliverTrophy />
          </div>
        )
      case 3:
        return (
          <div className="flex justify-center">
            <BronzeTrophy />
          </div>
        )
      default:
        return (
          <div className="flex justify-center text-sm font-medium text-neutral-700">
            {rank}
          </div>
        )
    }
  }

  const navigate = useNavigate()

  return (
    <div className="grid grid-cols-12 gap-y-3">
      <div className="col-span-2 flex justify-center text-sm font-medium text-neutral-600">
        排名
      </div>
      <div className="col-span-10 text-sm font-medium text-neutral-600">
        笔记
      </div>
      {rankList.map((item, index) => (
        <div
          key={item.noteId}
          className="col-span-12 grid grid-cols-12 items-center gap-2"
        >
          <div className="col-span-2">{rankMap(index + 1)}</div>
          <div className="col-span-10 flex flex-col gap-1 overflow-x-hidden">
            <div
              className={`truncate text-sm font-medium text-neutral-700 ${item.question ? 'cursor-pointer hover:text-blue-600' : ''}`}
              onClick={() => {
                if (item.question) {
                  navigate(`${QUESTION}/${item.question.questionId}`)
                }
              }}
              title={item.question?.title || ''}
            >
              {item.question?.title || '（未关联题目）'}
            </div>
            <div
              className="cursor-pointer text-sm text-neutral-500 hover:text-neutral-700"
              onClick={() => {
                navigate(`${NOTE_DETAIL}/${item.noteId}`)
              }}
              title={item.displayContent || item.content || ''}
            >
              {item.displayContent || item.content}
            </div>
            <div className="flex items-center gap-2">
              <Avatar
                src={item.author.avatarUrl}
                size={18}
                className={
                  'flex-shrink-0 cursor-pointer ' +
                  (item.author.avatarUrl === null ? 'bg-orange-300' : '')
                }
                onClick={() => {
                  navigate(`${USER_HOME}/${item.author.userId}`)
                }}
              >
                <UserOutlined />
              </Avatar>
              <span
                className="cursor-pointer text-xs text-neutral-500 hover:text-neutral-700"
                onClick={() => {
                  navigate(`${USER_HOME}/${item.author.userId}`)
                }}
              >
                {item.author.username}
              </span>
              <span className="ml-auto flex items-center gap-3 text-xs text-neutral-400">
                <span className="flex items-center gap-0.5 text-orange-500">
                  <FireOutlined />
                  {Math.round(item.hotScore)}
                </span>
              </span>
            </div>
          </div>
        </div>
      ))}
      {rankList.length === 0 && showEmpty && (
        <div className="col-span-12 text-center text-sm font-medium text-neutral-800">
          暂无热度数据
        </div>
      )}
    </div>
  )
}

export default NoteHotRankList
