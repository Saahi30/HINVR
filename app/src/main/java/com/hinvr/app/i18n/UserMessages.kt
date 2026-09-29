package com.hinvr.app.i18n

import android.content.Context
import com.hinvr.app.R

fun Context.userMessage(raw: String?, fallback: Int = R.string.err_generic): String {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return getString(fallback)
    val id = KnownMessages[text] ?: KnownMessages[text.replace('\'', '’')] ?: return text
    return getString(id)
}

private val KnownMessages = mapOf(
    "Sign in again." to R.string.err_sign_in_again,
    "Couldn’t save your profile." to R.string.err_save_profile,
    "Couldn’t save the account." to R.string.err_save_account,
    "Couldn’t save that place." to R.string.err_save_place,
    "Couldn't refresh the pass." to R.string.err_refresh_pass,
    "Couldn’t refresh the pass." to R.string.err_refresh_pass,
    "Couldn't open Google Wallet." to R.string.err_wallet,
    "Couldn’t open Google Wallet." to R.string.err_wallet,
    "Couldn't join the card list." to R.string.err_card_list,
    "Couldn’t join the card list." to R.string.err_card_list,
    "Couldn't send the request." to R.string.err_send_request,
    "Couldn’t send the request." to R.string.err_send_request,
    "Couldn’t read your location. Type the address." to R.string.err_location,
    "Location is off. Type the address." to R.string.err_location_off,
    "Location isn’t available on this phone." to R.string.err_location_unavailable,
    "Turn on location, or type the address." to R.string.err_location_enable,
    "Couldn’t turn that location into an address. Type it instead." to R.string.err_location_address,
    "Check your email to finish creating the account." to R.string.err_check_email,
    "You're already on the physical card list." to R.string.err_already_card,
    "You’re already on the physical card list." to R.string.err_already_card,
    "HINVR isn’t connected. Try again." to R.string.err_not_connected,
    "That email already has an account. Sign in instead." to R.string.err_email_taken,
    "That email or password doesn’t match." to R.string.err_credentials,
    "That email doesn’t look right." to R.string.err_email,
    "Choose a stronger password. At least 6 characters." to R.string.err_password,
    "Too many tries. Wait a minute, then try again." to R.string.err_rate,
    "No network. Try again." to R.string.err_network,
    "Couldn’t reach HINVR. Try again." to R.string.err_reach,
    "Couldn't open membership." to R.string.err_membership,
    "Couldn't open the browser." to R.string.err_browser,
    "Couldn’t use that link." to R.string.err_use_link,
    "Couldn’t send the reset link." to R.string.err_send_reset,
    "That link has expired. Request a new one from sign in." to R.string.err_link_expired,
    "Couldn’t save the password." to R.string.err_save_password,
)
