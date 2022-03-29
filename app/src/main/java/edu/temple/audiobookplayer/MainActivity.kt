package edu.temple.audiobookplayer

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity(), BookListFragment.BookFragmentInterface {

    private val isSingleContainer : Boolean by lazy{
        findViewById<View>(R.id.container2) == null
    }

    private val selectedBookViewModel: BookViewModel by lazy{
        ViewModelProvider(this).get(BookViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // creation of our book list from our strings.xml
        val authors = resources.getStringArray(R.array.book_authors)
        val titles = resources.getStringArray(R.array.book_names)

        val books: ArrayList<Book> = ArrayList()

//        for(i in authors.indices){
//            books.add(Book(titles[i], authors[i]))
//        }

        val my_books: BookList = BookList(books)

        if(supportFragmentManager.findFragmentById(R.id.container1) is DisplayFragment){
            supportFragmentManager.popBackStack()
        }

        // our first instance of the activity/ fragment
        if(savedInstanceState == null){
            // pass our book list to the main container which is a fragment adapter for recyclerview
            supportFragmentManager.beginTransaction().add(
                R.id.container1, BookListFragment.newInstance(my_books)).commit()
        }
        //
        else if(isSingleContainer && selectedBookViewModel.getSelectedBook().value != null){
            supportFragmentManager.beginTransaction().replace(R.id.container1, DisplayFragment())
                .setReorderingAllowed(true).addToBackStack(null).commit()
        }

        // if we have two containers but no fragment added to container two
        if(!isSingleContainer && supportFragmentManager.findFragmentById(R.id.container2) !is DisplayFragment){
            supportFragmentManager.beginTransaction().add(R.id.container2,DisplayFragment())
                .commit()
        }

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