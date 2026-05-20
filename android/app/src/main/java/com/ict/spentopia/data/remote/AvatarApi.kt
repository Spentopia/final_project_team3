package com.ict.spentopia.data.remote

import retrofit2.http.GET

data class UserAvatarItemResponse(
    val id: String,
    val item_id: String,
    val name: String,
    val category: String,
    val slot_name: String?,
    val image_url: String?,
    val metadata_uri: String?,
    val is_equipped: Boolean?,
    val is_nft: Boolean?,
    val nft_mint_address: String?,
    val minted_to_wallet: String?,
    val collection_mint: String?,
    val acquired_at: String?
)

interface AvatarApi {
    @GET("/api/avatar/items")
    suspend fun getUserItems(): List<UserAvatarItemResponse>
}
