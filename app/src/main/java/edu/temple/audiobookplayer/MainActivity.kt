package edu.temple.audiobookplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // book view model to hold our livedata information to send to the fragment with onClick
        val bookViewModel = ViewModelProvider(this).get(BookViewModel::class.java)

        // send this information to our container fragment and add it to the backStack to allow back button
        bookViewModel.getSelectedBook().observe(this){
            if(findViewById<View>(R.id.container2) == null) {
                supportFragmentManager.beginTransaction().replace(R.id.container1, DisplayFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

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

}