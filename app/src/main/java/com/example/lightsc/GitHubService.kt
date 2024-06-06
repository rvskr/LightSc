import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface GitHubService {
    @GET("repos/{owner}/{repo}/releases/latest")
    @Headers("Accept: application/vnd.github.v3+json")
    fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<GitHubRelease>
}

data class GitHubRelease(
    val tag_name: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val browser_download_url: String,
    val name: String
)
