package com.kien.book.repository

import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookBasicInfo
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.model.dto.book.BookView
import org.apache.ibatis.annotations.Mapper

@Mapper
interface BookMapper {

    fun getById(id: Long): BookView?

    fun getCountByCondition(bookCondition: BookCondition): Int

    fun getListByCondition(bookCondition: BookCondition): List<BookView>

    fun save(book: Book): Int

    fun batchSaveWithSpecifiedId(books: List<Book>): Int

    fun batchSaveWithoutId(books: List<Book>): Int

    fun deleteLogically(id: Long): Int

    fun deleteBatchLogically(ids: List<Long>): Int

    fun update(book: Book): Int
}
