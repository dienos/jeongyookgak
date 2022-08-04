package com.jeongyookgak.jth.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jeongyookgak.jth.data.api.RESULT_OK
import com.jeongyookgak.jth.data.model.CategoryData
import com.jeongyookgak.jth.data.model.ProductionData
import com.jeongyookgak.jth.domain.model.remote.Production
import com.jeongyookgak.jth.domain.usecase.GetProductionsUseCase
import com.jeongyookgak.jth.presentation.R
import com.jeongyookgak.jth.presentation.di.PreferencesUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductionViewModel @Inject constructor(
    app: Application,
    private val getProductionsUseCase: GetProductionsUseCase
) : BaseViewModel(app) {
    private var productionDataByFiller: List<Production> = arrayListOf()
    val productionData = MutableLiveData<ProductionData>()
    val categoryData = MutableLiveData<CategoryData>()

    private fun joinFavoriteData(
        remoteData: ArrayList<Production>,
        localFavoriteData: ArrayList<String>?
    ): List<Production> {
        val result: ArrayList<Production> = remoteData

        remoteData.forEach {
            it.isFavorite = false
        }

        remoteData.forEachIndexed { index, production ->
            localFavoriteData?.forEach { localKey ->
                if(production.key == localKey) {
                    result[index].isFavorite = true
                }
            }
        }

        return result
    }

    fun getProductionsByCategory(key: String) {
        val list = productionDataByFiller.filter {
            it.categoryKey == key
        }

        productionData.value = ProductionData(
            joinFavoriteData(
                list as ArrayList<Production>,
                PreferencesUtil.getStringArrayPref(app) as ArrayList<String>
            )
        )
    }

    fun getProductions() {
        try {
            updateProgress(true)

            viewModelScope.launch {
                val response = getProductionsUseCase.invoke()

                if (response.code == RESULT_OK) {
                    categoryData.value = CategoryData(response.categories)

                    productionData.value = ProductionData(
                        joinFavoriteData(
                            response.productions as ArrayList<Production>,
                            PreferencesUtil.getStringArrayPref(app) as ArrayList<String>
                        )
                    )

                    productionData.value?.list?.let {
                        productionDataByFiller = it
                    }

                }
            }

            updateProgress(false)
        } catch (e: Exception) {
            e.message?.let {
                updateProgress(false)
                updateToast(it)
            } ?: updateToast(app.getString(R.string.network_error))
        }
    }
}