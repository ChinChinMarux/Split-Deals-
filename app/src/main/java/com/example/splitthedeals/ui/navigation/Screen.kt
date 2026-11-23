package com.example.splitthedeals.ui.navigation

object Screen {
    const val Home = "home"
    const val Split = "split?billId={billId}"
    const val Riwayat = "riwayat"
    const val Profile = "profile"

    const val BillDetail = "bill/{billId}"
    const val EditProfile = "edit_profile"

    fun splitNew() = "split?billId="
    fun splitEdit(billId: String) = "split?billId=$billId"
    fun billDetail(billId: String) = "bill/$billId"
}
