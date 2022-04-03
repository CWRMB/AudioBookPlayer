package edu.temple.audiobookplayer

import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.net.URL

class MainActivity : AppCompatActivity(), BookListFragment.BookFragmentInterface {

    private val isSingleContainer : Boolean by lazy{
        findViewById<View>(R.id.container2) == null
    }

    private val selectedBookViewModel: BookViewModel by lazy{
        ViewModelProvider(this).get(BookViewModel::class.java)
    }

    lateinit var searchButton : Button
    lateinit var my_books : BookList
    lateinit var books : ArrayList<Book>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchButton = findViewById<Button>(R.id.searchButton)

        // creation of our book list from our strings.xml
        val authors = resources.getStringArray(R.array.book_authors)
        val titles = resources.getStringArray(R.array.book_names)

        books = ArrayList()

//        for(i in authors.indices){
//            books.add(Book(titles[i], authors[i]))
//        }

        //initialize books of an empty array to prevent nullable data
        my_books = BookList(books)

        if(supportFragmentManager.findFragmentById(R.id.container1) is DisplayFragment){
            supportFragmentManager.popBackStack()
        }

        // our first instance of the activity/ fragment
        if(savedInstanceState == null){
            // pass our book list to the main container which is a fragment adapter for recyclerview
            supportFragmentManager.beginTransaction().add(
                R.id.container1, BookListFragment.newInstance(my_books)).commit()
        }
        else if(isSingleContainer && selectedBookViewModel.getSelectedBook().value != null){
            supportFragmentManager.beginTransaction().replace(R.id.container1, DisplayFragment())
                .setReorderingAllowed(true).addToBackStack(null).commit()
        }

        // if we have two containers but no fragment added to container two
        if(!isSingleContainer && supportFragmentManager.findFragmentById(R.id.container2) !is DisplayFragment){
            supportFragmentManager.beginTransaction().add(R.id.container2,DisplayFragment())
                .commit()
        }

        searchButton.setOnClickListener{
            onSearchRequested()
            //handleIntent(intent)
        }

    }

    // since our instance activity is singleTop we must override this function to
    // intercept the intent that is passed from android
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent!!)
    }

    // method to receive and handle our intent query information
    private fun handleIntent(intent: Intent){
        if(Intent.ACTION_SEARCH == intent.action){
            intent.getStringExtra(SearchManager.QUERY)?.also{ query ->
                lifecycleScope.launch(Dispatchers.Main){
                    fetchBook(query)
                }
            }
        }
    }

    suspend fun fetchBook(bookID: String){
        //Log.v("BookID",bookID)
        val jsonArray: JSONArray

        withContext(Dispatchers.IO){
            // usually put try catch block here to check for proper JSON format
            jsonArray = JSONArray(URL("https://kamorris.com/lab/cis3515/search.php?term=$bookID")
                .openStream()
                .bufferedReader()
                .readLine())
        }

        // assemble our book list information once received from the JSON array
        for(i in 0 until jsonArray.length()){
            books.add(Book(jsonArray.getJSONObject(i).getString("title"),
                jsonArray.getJSONObject(i).getString("author"),
                jsonArray.getJSONObject(i).getInt("id"),
                jsonArray.getJSONObject(i).getString("cover_url")))
        }

        my_books = BookList(books)
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