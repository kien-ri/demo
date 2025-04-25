package com.kien.book.repository

import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookView
import org.apache.ibatis.annotations.Mapper

@Mapper
interface BookMapper {

    fun getById(id: Long): BookView?

    fun getCountByCondition(bookCondition: BookCondition): Int

    fun getListByCondition(bookCondition: BookCondition): List<BookView>

    fun save(bookCreate: BookCreate): Int

    fun delete(id: Long): Int

    fun deleteBatch(ids: List<Long>): Int

    fun update(book: Book): Int
}