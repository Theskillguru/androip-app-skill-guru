// FirebaseTokenManager.kt
import androidx.compose.ui.input.key.Key.Companion.C
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.InputStream

object FirebaseTokenManager {

    fun getAccessToken(): String? {
        // Load the service account JSON file from assets
        val serviceAccountFile = "C:\\New folder\\theSkillGuru\\theSkillGuru\\.gradle\\theskillguru-d9fac-firebase-adminsdk-3jc4q-dfd72b4d83.json"
        val inputStream: InputStream = this::class.java.classLoader?.getResourceAsStream(serviceAccountFile)
            ?: throw Exception("Service account file not found")

        val credentials: GoogleCredentials = ServiceAccountCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        return credentials.refreshAccessToken().tokenValue


        //C:\New folder\theSkillGuru\theSkillGuru\.gradle  C:\\New folder\\theSkillGuru\\theSkillGuru\\.gradle\\theskillguru-d9fac-firebase-adminsdk-3jc4q-dfd72b4d83.json"
    }
}
