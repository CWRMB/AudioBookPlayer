package edu.temple.audiobookplayer

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.text.Layout
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import edu.temple.audlibplayer.PlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity(), BookListFragment.BookFragmentInterface, ControlFragment.ControlFragmentInterface {

    private val isSingleContainer : Boolean by lazy{
        findViewById<View>(R.id.container2) == null
    }

    private val selectedBookViewModel: BookViewModel by lazy{
        ViewModelProvider(this).get(BookViewModel::class.java)
    }

    lateinit var searchButton : Button
    lateinit var my_books : BookList
    lateinit var playButton: Button
    lateinit var books : ArrayList<Book>

    lateinit var seekProgress: String

    var isConnected = false
    lateinit var audioBinder: PlayerService.MediaControlBinder

    val audioHandler = Handler(Looper.getMainLooper()){
        seekProgress = it.what.toString()
        true
    }

    val serviceConnection = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isConnected = true
            audioBinder = service as PlayerService.MediaControlBinder
            audioBinder.setProgressHandler(audioHandler)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchButton = findViewById<Button>(R.id.searchButton)

        // creation of our book list from our strings.xml
        //val authors = resources.getStringArray(R.array.book_authors)
        //val titles = resources.getStringArray(R.array.book_names)

        books = ArrayList()

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

            supportFragmentManager.beginTransaction().add(
                R.id.AudioControls, ControlFragment()).commit()
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
        bindService(Intent(this, PlayerService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE)
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

    // method for fetching the book typed in and uploading it to our fragments
    suspend fun fetchBook(bookID: String){
        //Log.v("BookID",bookID)
        val jsonArray: JSONArray
        // this gets the url containing each duration JSON ARRAY
        var jsonDuration: JSONObject
        // this will hold an array of duration ints i did it this way since its never specified
        // if I will be searching by ID or book name so I am assuming book name and this is
        // the work around
        var array_of_duration = arrayListOf<Int>()

        withContext(Dispatchers.IO){
            // usually put try catch block here to check for proper JSON format
            jsonArray = JSONArray(URL("https://kamorris.com/lab/cis3515/search.php?term=$bookID")
                .openStream()
                .bufferedReader()
                .readLine())

            // method to get the duration from the id which we acquire above
            var id : Int

            // get the other id links information from each ID
            for(i in 0 until jsonArray.length()){
                id = jsonArray.getJSONObject(i).getInt("id")
                // get the Json object and extract each duration from each book id
                jsonDuration = JSONObject(URL("https://kamorris.com/lab/cis3515/book.php?id=$id")
                    .openStream().bufferedReader().readLine())

                array_of_duration.add(jsonDuration.getInt("duration"))
            }
        }

        books = ArrayList()

        // assemble our book list information once received from the JSON array
        for(i in 0 until jsonArray.length()){
            books.add(Book(jsonArray.getJSONObject(i).getString("title"),
                jsonArray.getJSONObject(i).getString("author"),
                jsonArray.getJSONObject(i).getInt("id"),
                jsonArray.getJSONObject(i).getString("cover_url"),
                array_of_duration[i]))
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

    // receive information from our fragments buttons
    override fun play() {
        Log.v("message","PLAY")
        if((isConnected)){
            audioBinder.play(1)
        }
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}