package edu.temple.audiobookplayer

import java.io.Serializable

// Our data class must be Serializable to properly retrieve the information at runtime
// the reason for this is because the bundle in onCreate and in newInstance for our companion object
// both do not have any functions for passing in objects, so Serializable will pass in a byte stream instead
data class Book(val title: String, val author: String, val id: Int, val coverURL: String): Serializable {
}