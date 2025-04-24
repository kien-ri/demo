package com.kien.book.repository

import com.kien.book.model.condition.BookCondition
import com.kien.book.model.view.BookView
import org.apache.ibatis.annotations.Mapper

@Mapper
interface BookMapper {

    fun getById(id: Long): BookView?

    fun getListByCondition(bookCondition: BookCondition): List<BookView>

    fun getCountByCondition(bookCondition: BookCondition): Int
}