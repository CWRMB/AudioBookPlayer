package edu.temple.audiobookplayer

data class BookList(var books: ArrayList<Book>) {

    fun add(Book: Book){
        books.add(Book)
    }

    fun remove(Book: Book){
        books.remove(Book)
    }

    fun get(index: Int): Book{
        return books[index]
    }

    fun size(): Int{
        return books.size
    }

}