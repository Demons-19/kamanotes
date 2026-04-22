import React, { useEffect, useState } from 'react'
import { NoteWithRelations } from '../types/serviceTypes.ts'
import { useNavigate } from 'react-router-dom'
import { NOTE_DETAIL } from '../../../apps/user/router/config.ts'

import AuthorCard from './AuthorCard.tsx'
import { QuestionCard } from '../../question'
import NoteContent from './NoteContent.tsx'
import DisplayContent from './DisplayContent.tsx'
import ExpandButton from './ExpandButton.tsx'
import { Button, Divider, Popconfirm } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import OptionsCard from './OptionsCard.tsx'

interface NoteItemProps {
  note?: NoteWithRelations
  setNoteLikeStatus?: (noteId: number, isLiked: boolean) => void
  toggleIsModalOpen: () => void
  handleCollectionQueryParams: (noteId: number) => void
  handleSelectedNoteId: (noteId: number) => void
  showAuthor?: boolean
  showQuestion?: boolean
  showOptions?: boolean
  onRefresh?: () => void
  canDelete?: boolean
  onDeleteNote?: (noteId: number) => Promise<void>
}

const NoteItem: React.FC<NoteItemProps> = ({
  note,
  setNoteLikeStatus,
  showAuthor = true,
  showQuestion = true,
  showOptions = true,
  toggleIsModalOpen,
  handleCollectionQueryParams,
  handleSelectedNoteId,
  onRefresh,
  canDelete = false,
  onDeleteNote,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const navigate = useNavigate()

  const toggleCollapsed = () => {
    setIsCollapsed(!isCollapsed)
  }

  useEffect(() => {
    if (note?.needCollapsed) {
      setIsCollapsed(true)
    }
  }, [])

  const handleDelete = async () => {
    if (!note || !onDeleteNote) return
    setDeleting(true)
    try {
      await onDeleteNote(note.noteId)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <div className="flex w-full flex-col gap-4">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            {showQuestion && <QuestionCard question={note?.question} />}
            {showAuthor && <AuthorCard note={note} />}
          </div>
          {canDelete && note && onDeleteNote && (
            <Popconfirm
              title="删除这篇笔记？"
              description="删除后无法恢复。"
              okText="确认删除"
              cancelText="取消"
              onConfirm={handleDelete}
            >
              <Button
                danger
                type="text"
                icon={<DeleteOutlined />}
                loading={deleting}
                className="mt-1"
              >
                删除
              </Button>
            </Popconfirm>
          )}
        </div>
        {isCollapsed ? (
          <DisplayContent displayContent={note?.displayContent ?? ''} />
        ) : (
          <div
            className="cursor-pointer"
            onClick={() => {
              if (note?.noteId) {
                navigate(`${NOTE_DETAIL}/${note.noteId}`)
              }
            }}
          >
            <NoteContent note={note} />
          </div>
        )}
        {note?.needCollapsed && (
          <ExpandButton
            toggleCollapsed={toggleCollapsed}
            isCollapsed={isCollapsed}
            key={note?.noteId}
          />
        )}
        {showOptions && (
          <OptionsCard
            note={note}
            setNoteLikeStatus={setNoteLikeStatus}
            toggleIsModalOpen={toggleIsModalOpen}
            handleCollectionQueryParams={handleCollectionQueryParams}
            handleSelectedNoteId={handleSelectedNoteId}
            onRefresh={onRefresh}
          />
        )}
      </div>
      <Divider />
    </>
  )
}

export default NoteItem
