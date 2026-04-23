import React, { useEffect, useState } from 'react'
import { NoteItem, NoteQueryParams } from '../index.ts'
import { CollectionQueryParams } from '../../collection/types/types.ts'
import { useCollection2 } from '../../collection'
import { useApp } from '@/base/hooks'
import { useUser } from '../../user/hooks/useUser.ts'
import CollectionModal from '../../collection/components/CollectionModal.tsx'
import { NoteWithRelations } from '../types/serviceTypes.ts'
import { Pagination as PaginationType } from '../../../request'
import { Empty, Pagination } from 'antd'

interface NoteListProps {
  noteList: NoteWithRelations[]
  pagination: PaginationType | undefined
  setNoteLikeStatusHandle: (noteId: number, isLiked: boolean) => void
  setNoteCollectStatusHandle: (noteId: number, isCollected: boolean) => void
  queryParams: NoteQueryParams
  setQueryParams: (queryParams: NoteQueryParams) => void
  showAuthor?: boolean
  showQuestion?: boolean
  showOptions?: boolean
  canDelete?: boolean
  onDeleteNote?: (noteId: number) => Promise<void>
}

const NoteList: React.FC<NoteListProps> = ({
  noteList,
  pagination,
  setNoteLikeStatusHandle,
  setNoteCollectStatusHandle,
  queryParams,
  setQueryParams,
  showOptions = true,
  showAuthor = true,
  showQuestion = true,
  canDelete = false,
  onDeleteNote,
}) => {
  function handlePageChange(page: number, pageSize: number) {
    setQueryParams({
      ...queryParams,
      page,
      pageSize,
    })
  }

  const [isModalOpen, setIsModalOpen] = useState(false)
  const toggleIsModalOpen = () => {
    setIsModalOpen(!isModalOpen)
  }

  const [selectedNoteId, setSelectedNoteId] = useState<number | undefined>(
    undefined,
  )
  const handleSelectedNoteId = (noteId: number) => {
    setSelectedNoteId(noteId)
  }

  const app = useApp()
  const user = useUser()

  const [collectionQueryParams, setCollectionQueryParams] =
    useState<CollectionQueryParams>({
      noteId: undefined,
      creatorId: undefined,
    })

  useEffect(() => {
    setCollectionQueryParams((prev) => {
      return {
        ...prev,
        creatorId: app.isLogin ? user.userId : undefined,
      }
    })
  }, [app, user])

  function handleCollectionQueryParams(noteId: number) {
    setCollectionQueryParams((prev) => {
      return {
        ...prev,
        noteId,
      }
    })
  }

  const { collectionVOList, createCollection, collectNote } = useCollection2(
    collectionQueryParams,
  )

  return (
    <div>
      {noteList.map((note) => (
        <NoteItem
          key={note.noteId}
          note={note}
          setNoteLikeStatus={setNoteLikeStatusHandle}
          toggleIsModalOpen={toggleIsModalOpen}
          handleCollectionQueryParams={handleCollectionQueryParams}
          handleSelectedNoteId={handleSelectedNoteId}
          showOptions={showOptions}
          showAuthor={showAuthor}
          showQuestion={showQuestion}
          canDelete={canDelete}
          onDeleteNote={onDeleteNote}
        />
      ))}
      {noteList.length > 0 && (
        <div className="flex justify-center">
          <Pagination
            current={queryParams.page ?? 1}
            pageSize={queryParams.pageSize ?? pagination?.pageSize}
            total={pagination?.total}
            onChange={handlePageChange}
          />
        </div>
      )}
      {noteList.length === 0 && <Empty description={'暂无笔记'} />}
      <CollectionModal
        isModalOpen={isModalOpen}
        collectNote={collectNote}
        selectedNoteId={selectedNoteId}
        toggleIsModalOpen={toggleIsModalOpen}
        collectionVOList={collectionVOList}
        createCollection={createCollection}
        setNoteCollectStatusHandle={setNoteCollectStatusHandle}
      />
    </div>
  )
}

export default NoteList
