import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FCMApiService {
    @POST("v1/projects/theskillguru-d9fac/messages:send")
    fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body message: FCMMessage
    ): Call<FCMResponse>
}

data class FCMMessage(
    val message: Message
)

data class Message(
    val token: String,
    val notification: Notification,
    val data: Map<String, String>
)

data class Notification(
    val title: String,
    val body: String
)

data class FCMResponse(
    val name: String
)