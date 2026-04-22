import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Divider, Empty, Spin, message } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { Panel } from '../../../../base/components'
import { noteService } from '../../../../domain/note/service/noteService.ts'
import { NoteWithRelations } from '../../../../domain/note/types/serviceTypes.ts'
import AuthorCard from '../../../../domain/note/components/AuthorCard.tsx'
import NoteContent from '../../../../domain/note/components/NoteContent.tsx'
import DisplayContent from '../../../../domain/note/components/DisplayContent.tsx'
import ExpandButton from '../../../../domain/note/components/ExpandButton.tsx'
import { QuestionCard } from '../../../../domain/question'

const NoteDetailPage: React.FC = () => {
  const { noteId } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [note, setNote] = useState<NoteWithRelations | null>(null)
  const [isCollapsed, setIsCollapsed] = useState(false)

  const fetchNoteDetail = async () => {
    if (!noteId) return
    setLoading(true)
    try {
      const { data } = await noteService.getNoteDetailService(Number(noteId))
      setNote(data)
      setIsCollapsed(Boolean(data.needCollapsed))
    } catch (error: any) {
      setNote(null)
      message.error(error?.message || '加载笔记详情失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchNoteDetail().then()
  }, [noteId])

  return (
    <div className="mx-auto w-[800px]">
      <Panel>
        <div className="mb-4 flex items-center gap-3">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
            返回
          </Button>
          <div className="text-lg font-semibold text-neutral-800">笔记详情</div>
        </div>

        {loading ? (
          <div className="py-12 text-center">
            <Spin tip="加载中..." />
          </div>
        ) : !note ? (
          <Empty description="笔记不存在或加载失败" />
        ) : (
          <div className="flex flex-col gap-4">
            <QuestionCard question={note.question} />
            <AuthorCard note={note} />
            {isCollapsed ? (
              <DisplayContent displayContent={note.displayContent ?? ''} />
            ) : (
              <NoteContent note={note} />
            )}
            {note.needCollapsed && (
              <ExpandButton
                isCollapsed={isCollapsed}
                toggleCollapsed={() => setIsCollapsed((prev) => !prev)}
              />
            )}
            <Divider className="my-2" />
            <div className="flex items-center gap-6 text-sm text-neutral-500">
              <span>点赞 {note.likeCount ?? 0}</span>
              <span>收藏 {note.collectCount ?? 0}</span>
              <span>评论 {note.commentCount ?? 0}</span>
            </div>
          </div>
        )}
      </Panel>
    </div>
  )
}

export default NoteDetailPage
