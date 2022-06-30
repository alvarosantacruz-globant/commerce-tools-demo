package com.asct94.commercetools

import io.vrap.rmf.base.client.AuthenticationToken
import io.vrap.rmf.base.client.oauth2.TokenStorage

object TokenHolder : TokenStorage {

    var _token: AuthenticationToken? = null

    override fun getToken(): AuthenticationToken? {
        return _token
    }

    override fun setToken(token: AuthenticationToken?) {
        this._token = token
    }
}