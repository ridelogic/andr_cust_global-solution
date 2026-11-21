package com.arwe.newproject.ui.addresses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.databinding.ItemMyAddressBinding

class MyAddressesAdapter(
    private val onEdit: (CustomerAddressResponse) -> Unit,
    private val onDelete: (CustomerAddressResponse) -> Unit,
    private val onSetDefault: (CustomerAddressResponse) -> Unit
) : RecyclerView.Adapter<MyAddressesAdapter.AddressViewHolder>() {

    private val addresses = mutableListOf<CustomerAddressResponse>()

    fun submitList(newAddresses: List<CustomerAddressResponse>) {
        addresses.clear()
        addresses.addAll(newAddresses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemMyAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int = addresses.size

    inner class AddressViewHolder(private val binding: ItemMyAddressBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(address: CustomerAddressResponse) {
            val context = binding.root.context

            binding.addressLabelText.text = address.addressType
            binding.addressLine1Text.text = address.addressLine1

            val line2 = address.addressLine2?.takeIf { it.isNotBlank() }
            binding.addressLine2Text.text = line2
            binding.addressLine2Text.visibility = if (line2 != null) View.VISIBLE else View.GONE

            val landmark = address.landmark?.takeIf { it.isNotBlank() }
            binding.landmarkText.text = landmark?.let { context.getString(R.string.my_addresses_landmark_format, it) }
            binding.landmarkText.visibility = if (landmark != null) View.VISIBLE else View.GONE

            binding.cityStatePostalText.text = context.getString(
                R.string.my_addresses_city_state_postal_format,
                address.city,
                address.state,
                address.postalCode
            )

            binding.defaultBadge.visibility = if (address.isDefault) View.VISIBLE else View.GONE
            binding.setDefaultAction.visibility = if (address.isDefault) View.GONE else View.VISIBLE

            binding.editAction.setOnClickListener { onEdit(address) }
            binding.deleteAction.setOnClickListener { onDelete(address) }
            binding.setDefaultAction.setOnClickListener { onSetDefault(address) }
        }
    }
}
