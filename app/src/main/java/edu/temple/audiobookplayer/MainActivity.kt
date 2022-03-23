package edu.temple.audiobookplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity(), BookListFragment.BookFragmentInterface {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // creation of our book list from our strings.xml
        val authors = resources.getStringArray(R.array.book_authors)
        val titles = resources.getStringArray(R.array.book_names)

        val books: ArrayList<Book> = arrayListOf(Book("",""))

        for(i in authors.indices){
            books.add(Book(titles[i], authors[i]))
        }

        val my_books: BookList = BookList(books)

        // pass our book list to the main container which is a fragment adapter for recyclerview
        supportFragmentManager.beginTransaction().replace(
            R.id.container1, BookListFragment.newInstance(my_books)).commit()

    }

    // use an interface nested inside our BookListFragment class to pass our data to the fragment
    // we use this so it doesn't observe instance changes
    override fun bookSelected() {
        if(findViewById<View>(R.id.container2) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container1, DisplayFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}