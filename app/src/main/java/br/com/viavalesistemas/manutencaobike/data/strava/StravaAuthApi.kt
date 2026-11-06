package br.com.viavalesistemas.manutencaobike.data.strava

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface StravaAuthApi {
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun trocarCodigo(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") codigo: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): TokenDto

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun renovar(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenDto

    @FormUrlEncoded
    @POST("oauth/deauthorize")
    suspend fun desautorizar(@Field("access_token") accessToken: String)
}
