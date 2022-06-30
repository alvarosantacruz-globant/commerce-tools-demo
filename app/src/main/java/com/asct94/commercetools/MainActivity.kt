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
import com.commercetools.api.models.product.ProductPagedQueryResponse
import io.vrap.rmf.base.client.ApiHttpResponse
import io.vrap.rmf.base.client.oauth2.ClientCredentials
import java.util.concurrent.CompletableFuture

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
            getProducts()
        }

        findViewById<Button>(R.id.btMyCarts).setOnClickListener {
            showCarts()
        }

        findViewById<Button>(R.id.btActiveCart).setOnClickListener {
            activeCart()
        }

        findViewById<Button>(R.id.btNewCart).setOnClickListener {
            createCart()
        }

        findViewById<Button>(R.id.btAddProductToCart).setOnClickListener {
            addProductToCart()
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

    private fun getProducts() {
        val future: CompletableFuture<ApiHttpResponse<ProductPagedQueryResponse>> =
            api.products()
                .get()
                .execute()
        val response = future.get()
        val productList = response.body.results
        this.productList = productList

        Toast.makeText(this, "${productList.size} products", Toast.LENGTH_SHORT).show()

        Log.e("ASCT", productList.map { it.masterData.current.name.get("en-us") }.toString())
    }

    private fun showCarts() {
        val future = api.me().carts().get().execute()
        val myCarts = future.get().body.results

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

    private fun activeCart() {
        val future = api.me().activeCart().get().execute()
        val activeCart = future.get().body.get()

        saveActiveCart(activeCart)

        if (activeCart.id != null) {
            val lineItems = activeCart.lineItems.size
            Toast.makeText(this, "Active cart has $lineItems item(s)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No carts found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCart() {
        val cart = MyCartDraft.builder()
            .currency("USD")
            .build()
        val future = api.me().carts().post(cart).execute()
        val newCart = future.get().body.get()
        val newCartId = newCart.id

        saveActiveCart(newCart)

        Toast.makeText(this, "New Cart created!\n$newCartId", Toast.LENGTH_SHORT).show()
    }

    private fun addProductToCart() {
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
        val future = api.me().carts().withId(activeCartId).post(cartUpdate).execute()
        val updatedCart = future.get().body.get()
        saveActiveCart(updatedCart)

        Toast.makeText(this, "Cart updated!", Toast.LENGTH_SHORT).show()
    }

    private fun saveActiveCart(cart: Cart) {
        this.activeCartId = cart.id
        this.activeCartVersion = cart.version
    }
}