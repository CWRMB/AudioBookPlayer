package edu.temple.audiobookplayer

import android.app.DownloadManager
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
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
import java.io.*
import java.lang.Exception
import java.net.URL
import java.nio.file.Files
import java.nio.file.Files.exists
import java.nio.file.Paths

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

    var saveProgress: Int = 0

    var isConnected = false
    lateinit var audioBinder: PlayerService.MediaControlBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchButton = findViewById<Button>(R.id.searchButton)

        serviceIntent = Intent(this, PlayerService::class.java)

        // check if we have any saved books
        if(getBookList().isNotEmpty()){
            books = getBookList()
        }else{
            books = ArrayList()
        }

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

        // bind our service to this activity
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

            // could optimize this
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

    // is our service connected our disconnected
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
        // book file
        val filename = selectedBookViewModel.getSelectedBook().value?.id.toString()
        val file = File(filesDir, filename)

        // progress file
        val progressFileName = ("Progress").plus(filename)
        val progressFile = File(filesDir, progressFileName)

        // play book from downloaded file
        if(file.exists()){
            // set the playing book
            selectedBookViewModel.getSelectedBook().observe(this){
                selectedBookViewModel.setPlayingBook(it)
            }

            var startPosition: Int = 0
            // if the progress file exists set the progress to what is saved
            if(progressFile.exists()){
                // get progress at first index since we only saved 1 number
                saveProgress = progressFile.readLines()[0].toInt()
                Log.v("RECOVERED PROGRESS", saveProgress.toString())

                // get the ratio of the seek bar to the duration of the book
                selectedBookViewModel.getPlayingBook().value?.also{
                    startPosition = ((saveProgress.div(100.00)) * it.duration).toInt()
                }
            }

            // this part is responsible for moving the sliding according to the progress of the book
            val audioHandler = Handler(Looper.getMainLooper()) {msg ->

                msg.obj?.let { msgObj ->
                    val bookProgress = msgObj as PlayerService.BookProgress

                    supportFragmentManager.findFragmentById(R.id.AudioControls)?.run {
                        with(this as ControlFragment) {
                            selectedBookViewModel.getPlayingBook().value?.also {
                                saveProgress = ((bookProgress.progress / it.duration.toFloat()) * 100).toInt()
                                setPlayProgress(saveProgress)
                            }
                        }
                    }
                }
                true
            }
            // pass the handler
            audioBinder.setProgressHandler(audioHandler)

            Log.v("START POSITION", startPosition.toString())
            audioBinder.play(file,startPosition)
            Log.v("DOWNLOADED FILE PLAY","TRUE")

            startService(serviceIntent)
            return
        }

        // stream book from id and download on background thread
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
                                saveProgress = ((bookProgress.progress / it.duration.toFloat()) * 100).toInt()
                                setPlayProgress(saveProgress)
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

            // download book
            var id: Int = 0
            selectedBookViewModel.playingBook.value?.let{ id = it.id}
            downloadBook(URL("https://kamorris.com/lab/audlib/download.php?id=$id"), id.toString())

            startService(serviceIntent)
        }
    }

    override fun pause() {
        if((isConnected)){
            audioBinder.pause()
            savePosition()
        }
    }

    override fun stop() {
        if((isConnected)){
            // set the seekBar progress to 0
            supportFragmentManager.findFragmentById(R.id.AudioControls)?.run {
                with(this as ControlFragment) {
                    selectedBookViewModel.getPlayingBook().value?.also {
                        setPlayProgress(0)
                        saveProgress = 0
                        savePosition()
                    }
                }
            }
            // stop service and streaming
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
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onStop(){
        savePosition()
        saveBookList()
        super.onStop()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        savePosition()
    }

    private fun getBookList(): ArrayList<Book>{
        val bookList: ArrayList<Book> = ArrayList()
        try {
            val file = File(filesDir, "BookList")
            if (file.exists()) {
                val fileInputStream = FileInputStream(file)
                val objectInputStream = ObjectInputStream(fileInputStream)
                val count = objectInputStream.readInt()
                // count is the number of books saved
                for (i in 0 until count) {
                    bookList.add(objectInputStream.readObject() as Book)
                }

            }
        }
        catch(e: Exception){
            e.printStackTrace()
        }
        return bookList
    }

    private fun saveBookList(){
        try {
            // save the book under the file of the ID plus progress
            val filename = "BookList"
            val file = File(filesDir, filename)
            val outputStream = FileOutputStream(file)
            val objectOutputStream = ObjectOutputStream(outputStream)
            objectOutputStream.writeInt(my_books.books.size)
            for (book in my_books.books) {
                objectOutputStream.writeObject(book)
            }
            Log.v("Books SAVED", "TRUE")
            outputStream.close()
        }
        catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun savePosition(){
        try{
            // save the book under the file of the ID plus progress
            val filename = ("Progress").plus(selectedBookViewModel.getPlayingBook().value?.id.toString())
            val file = File(filesDir, filename)
            val outputStream = FileOutputStream(file)
            outputStream.write(saveProgress.toString().toByteArray())
            Log.v("Progress SAVED", saveProgress.toString())
            outputStream.close()
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }

    // download book
    private fun downloadBook(url: URL, filename: String){
        lifecycleScope.launch(Dispatchers.IO){
            url.openStream().use { input ->
                FileOutputStream(File(filesDir,filename)).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}