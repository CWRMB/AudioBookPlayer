package edu.temple.audiobookplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val authors = resources.getStringArray(R.array.book_authors)
        val titles = resources.getStringArray(R.array.book_names)

        val books: ArrayList<Book> = arrayListOf(Book("",""))

        for(i in authors.indices){
            books.add(Book(authors[i], titles[i]))
        }

        val my_books: BookList = BookList(books)

        supportFragmentManager.beginTransaction().add(
            R.id.container1, BookListFragment.newInstance(my_books)).commit()
    }

}