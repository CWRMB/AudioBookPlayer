package edu.temple.audiobookplayer

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider

class ControlFragment : Fragment(){

    lateinit var playButton: Button
    lateinit var pauseButton: Button
    lateinit var stopButton: Button
    lateinit var bookTitle: TextView
    lateinit var seekBar: SeekBar
    var isPaused = false

    private val bookViewModel : BookViewModel by lazy{
        ViewModelProvider(requireActivity()).get(BookViewModel::class.java)
    }

    // save pause info across state changes
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isPaused",isPaused)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // restore our pause information
        if(savedInstanceState != null){
            isPaused = savedInstanceState.getBoolean("isPaused")
        }

        // set on click listener for each button
        playButton = view.findViewById(R.id.button_play)
        pauseButton = view.findViewById(R.id.button_pause)
        stopButton = view.findViewById(R.id.button_stop)
        bookTitle = view.findViewById(R.id.titleDisplay)
        seekBar = view.findViewById(R.id.bookProgress)

        seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                if(fromUser){
                    (requireActivity() as ControlFragment.ControlFragmentInterface).progress(progress)
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar) {

            }

            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })

        // functionality that passes the button presses to our interface to do service interaction with
        // these also change the title of the text view to display the playing title
        playButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).play()
            bookViewModel.getPlayingBook().observe(requireActivity()){
                bookTitle.text = it.title.plus(" (Now Playing)")
            }
        }

        pauseButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).pause()
            if(!isPaused){
                bookViewModel.getPlayingBook().observe(requireActivity()){
                    bookTitle.text = it.title.plus(" (Paused)")
                    isPaused = true
                }
            }
            else if(isPaused){
                bookViewModel.getPlayingBook().observe(requireActivity()){
                    bookTitle.text = it.title.plus(" (Now Playing)")
                    isPaused = false
                }
            }

        }

        stopButton.setOnClickListener {
            (requireActivity() as ControlFragment.ControlFragmentInterface).stop()
            bookTitle.text = ""
        }

        //set the text for playing book
        bookViewModel.getPlayingBook().observe(requireActivity()){
            if(isPaused){
                bookTitle.text = it.title.plus(" (Paused)")
            }
            else{
                bookTitle.text = it.title.plus(" (Now Playing)")
            }
        }
    }

    fun setPlayProgress(progress: Int){
        seekBar.setProgress(progress, true)
    }

    // interface to hold abstract data to be implemented into main
    public interface ControlFragmentInterface {
        fun play()
        fun pause()
        fun stop()
        fun progress(progress: Int)
    }
}