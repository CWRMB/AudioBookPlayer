package edu.temple.audiobookplayer

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.service.controls.Control
import android.text.Layout
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.fragment.app.Fragment
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

    private lateinit var serviceIntent: Intent

    lateinit var searchButton : Button
    lateinit var my_books : BookList
    lateinit var books : ArrayList<Book>

    var seekProgress: Int = 0

    var isConnected = false
    lateinit var audioBinder: PlayerService.MediaControlBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchButton = findViewById<Button>(R.id.searchButton)

        serviceIntent = Intent(this, PlayerService::class.java)

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

    val serviceConnection = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isConnected = true
            audioBinder = service as PlayerService.MediaControlBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
        }
    }

    // receive information from our fragments buttons
    override fun play() {

        if((isConnected)){
            // set the playing book
            selectedBookViewModel.getSelectedBook().observe(this){
                selectedBookViewModel.setPlayingBook(it)
            }

            // this part is responsible for moving the sliding according to the progress of the book
            val audioHandler = Handler(Looper.getMainLooper()) {msg ->

                msg.obj?.let { msgObj ->
                    val bookProgress = msgObj as PlayerService.BookProgress

                    supportFragmentManager.findFragmentById(R.id.AudioControls)?.run {
                        with(this as ControlFragment) {
                            selectedBookViewModel.getPlayingBook().value?.also {
                                setPlayProgress(((bookProgress.progress / it.duration.toFloat()) * 100).toInt())
                            }
                        }
                    }
                }
                true
            }
            // pass the handler
            audioBinder.setProgressHandler(audioHandler)
            //set the playing book
            selectedBookViewModel.selectedBook.value?.let { audioBinder.play(it.id)}
            startService(serviceIntent)
        }
    }

    override fun pause() {
        if((isConnected)){
            audioBinder.pause()
        }
    }

    override fun stop() {
        if((isConnected)){
            audioBinder.stop()
            stopService(serviceIntent)
        }
    }

    // change progress of the book from the seek slider
    override fun progress(progress: Int) {
        val duration = selectedBookViewModel.selectedBook.value?.duration

        // get percentage
        val ratio = progress.div(100.00)

        if (duration != null) {
            audioBinder.seekTo((duration * ratio).toInt())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}