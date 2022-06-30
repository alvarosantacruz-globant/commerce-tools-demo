package com.asct94.commercetools

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.defaultconfig.ApiRootBuilder
import com.commercetools.api.defaultconfig.ServiceRegion
import com.commercetools.api.models.cart.Cart
import com.commercetools.api.models.me.MyCartDraft
import com.commercetools.api.models.me.MyCartUpdate
import com.commercetools.api.models.me.MyCartUpdateAction
import com.commercetools.api.models.product.Product
import io.vrap.rmf.base.client.oauth2.ClientCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var api: ProjectApiRoot
    private var productList: List<Product>? = null
    private var activeCartId: String? = null
    private var activeCartVersion: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupSdkAnonymous()

        findViewById<Button>(R.id.btProducts).setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) { getProducts() }
        }

        findViewById<Button>(R.id.btMyCarts).setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) { showCarts() }
        }

        findViewById<Button>(R.id.btActiveCart).setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) { activeCart() }
        }

        findViewById<Button>(R.id.btNewCart).setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) { createCart() }
        }

        findViewById<Button>(R.id.btAddProductToCart).setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) { addProductToCart() }
        }
    }

    private fun setupSdkAnonymous() {
        api = ApiRootBuilder.of().withProjectKey(PROJECT_KEY).withAnonymousRefreshFlow(
            ClientCredentials.of()
                .withClientId(CLIENT_ID)
                .withClientSecret(CLIENT_SECRET)
                .withScopes(SCOPES)
                .build(),
            ServiceRegion.GCP_US_CENTRAL1,
            TokenHolder
        ).build(PROJECT_KEY)
    }

    private suspend fun getProducts() {
        val result = api.products().get().makeCall()
        val productList = result.results
        this.productList = productList

        Toast.makeText(this, "${productList.size} products", Toast.LENGTH_SHORT).show()

        Log.e("ASCT", productList.map { it.masterData.current.name.get("en-us") }.toString())
    }


    private suspend fun showCarts() {
        val result = api.me().carts().get().makeCall()
        val myCarts = result.results

        if (myCarts.size > 1) {
            Toast.makeText(this, "${myCarts.size} carts", Toast.LENGTH_SHORT).show()
        } else if (myCarts.size == 1) {
            val cart = myCarts.first()
            val lineItems = cart.lineItems.size
            Toast.makeText(this, "1 cart with $lineItems item(s)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No carts", Toast.LENGTH_SHORT).show()
        }

//        Log.e("ASCT", productList.map { it.masterData.current.name.get("en-us") }.toString())
    }

    private suspend fun activeCart() {
        val result = api.me().activeCart().get().makeCall()
        val activeCart = result.get()

        saveActiveCart(activeCart)

        if (activeCart.id != null) {
            val lineItems = activeCart.lineItems.size
            Toast.makeText(this, "Active cart has $lineItems item(s)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No carts found", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun createCart() {
        val cart = MyCartDraft.builder()
            .currency("USD")
            .build()
        val result = api.me().carts().post(cart).makeCall()
        val newCart = result.get()
        val newCartId = newCart.id

        saveActiveCart(newCart)

        Toast.makeText(this, "New Cart created!\n$newCartId", Toast.LENGTH_SHORT).show()
    }

    private suspend fun addProductToCart() {
        if (activeCartId == null) {
            Toast.makeText(this, "No active cart", Toast.LENGTH_SHORT).show()
            return
        } else if (productList.isNullOrEmpty()) {
            Toast.makeText(this, "Need to fetch products first", Toast.LENGTH_SHORT).show()
            return
        }

        val someProduct: Product = productList!!.last()
        val cartUpdate = MyCartUpdate.builder()
            .version(activeCartVersion)
            .actions(
                MyCartUpdateAction.addLineItemBuilder()
                    .productId(someProduct.id)
                    .variantId(someProduct.masterData.current.masterVariant.id)
                    .quantity(2)
                    .build()
            )
            .build()
        val result = api.me().carts().withId(activeCartId).post(cartUpdate).makeCall()
        val updatedCart = result.get()
        saveActiveCart(updatedCart)

        Toast.makeText(this, "Cart updated!", Toast.LENGTH_SHORT).show()
    }

    private fun saveActiveCart(cart: Cart) {
        this.activeCartId = cart.id
        this.activeCartVersion = cart.version
    }
}