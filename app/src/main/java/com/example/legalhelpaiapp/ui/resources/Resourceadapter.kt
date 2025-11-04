package com.example.legalhelpaiapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.legalhelpaiapp.databinding.ItemResourceBinding

class ResourceAdapter(
    private var resources: List<Resource>,
    private val onBookmarkClick: (Resource) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    private var filteredResources: List<Resource> = resources

    inner class ResourceViewHolder(private val binding: ItemResourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(resource: Resource) {
            binding.resourceTitle.text = resource.title
            binding.resourceDescription.text = resource.description
            binding.resourceCategory.text = resource.category
            binding.resourceIcon.setImageResource(resource.iconRes)

            // Phone
            if (resource.phone != null) {
                binding.phoneLayout.visibility = View.VISIBLE
                binding.resourcePhone.text = resource.phone
                binding.callButton.visibility = View.VISIBLE
            } else {
                binding.phoneLayout.visibility = View.GONE
                binding.callButton.visibility = View.GONE
            }

            // Website
            if (resource.website != null) {
                binding.websiteLayout.visibility = View.VISIBLE
                binding.resourceWebsite.text = resource.website
            } else {
                binding.websiteLayout.visibility = View.GONE
            }

            // Bookmark
            updateBookmarkIcon(resource.isBookmarked)
            binding.bookmarkButton.setOnClickListener {
                resource.isBookmarked = !resource.isBookmarked
                updateBookmarkIcon(resource.isBookmarked)
                onBookmarkClick(resource)
            }

            // Call button
            binding.callButton.setOnClickListener {
                resource.phone?.let { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    binding.root.context.startActivity(intent)
                }
            }

            // Visit button
            binding.visitButton.setOnClickListener {
                resource.website?.let { website ->
                    val url = if (website.startsWith("http")) website else "https://$website"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    binding.root.context.startActivity(intent)
                }
            }

            // Share button
            binding.shareButton.setOnClickListener {
                shareResource(resource)
            }
        }

        private fun updateBookmarkIcon(isBookmarked: Boolean) {
            if (isBookmarked) {
                binding.bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
            } else {
                binding.bookmarkButton.setImageResource(R.drawable.ic_bookmark_border)
            }
        }

        private fun shareResource(resource: Resource) {
            val shareText = buildString {
                append("${resource.title}\n\n")
                append("${resource.description}\n\n")
                if (resource.phone != null) {
                    append("Phone: ${resource.phone}\n")
                }
                if (resource.website != null) {
                    append("Website: ${resource.website}\n")
                }
                append("\nShared via Legal Help AI App")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, resource.title)
            }
            binding.root.context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val binding = ItemResourceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        holder.bind(filteredResources[position])
    }

    override fun getItemCount(): Int = filteredResources.size

    fun filter(query: String) {
        filteredResources = if (query.isEmpty()) {
            resources
        } else {
            resources.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun filterByCategory(category: String) {
        filteredResources = if (category == "All") {
            resources
        } else {
            resources.filter { it.category == category }
        }
        notifyDataSetChanged()
    }

    fun updateResources(newResources: List<Resource>) {
        resources = newResources
        filteredResources = newResources
        notifyDataSetChanged()
    }
}