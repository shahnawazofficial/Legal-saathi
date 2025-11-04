package com.example.legalhelpaiapp.ui.resources

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.legalhelpaiapp.R
import com.example.legalhelpaiapp.Resource
import com.example.legalhelpaiapp.ResourceAdapter
import com.example.legalhelpaiapp.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {

    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!

    private lateinit var emergencyAdapter: ResourceAdapter
    private lateinit var governmentAdapter: ResourceAdapter
    private lateinit var legalAidAdapter: ResourceAdapter
    private lateinit var helplinesAdapter: ResourceAdapter
    private lateinit var portalsAdapter: ResourceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearch()
        setupCategoryFilters()
    }

    private fun setupRecyclerViews() {
        // Emergency Resources
        val emergencyResources = getEmergencyResources()
        emergencyAdapter = ResourceAdapter(emergencyResources) { resource ->
            handleBookmark(resource)
        }
        binding.emergencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = emergencyAdapter
        }

        // Government Resources
        val governmentResources = getGovernmentResources()
        governmentAdapter = ResourceAdapter(governmentResources) { resource ->
            handleBookmark(resource)
        }
        binding.governmentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = governmentAdapter
        }

        // Legal Aid Resources
        val legalAidResources = getLegalAidResources()
        legalAidAdapter = ResourceAdapter(legalAidResources) { resource ->
            handleBookmark(resource)
        }
        binding.legalAidRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = legalAidAdapter
        }

        // Helplines
        val helplinesResources = getHelplinesResources()
        helplinesAdapter = ResourceAdapter(helplinesResources) { resource ->
            handleBookmark(resource)
        }
        binding.helplinesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = helplinesAdapter
        }

        // Online Portals
        val portalsResources = getPortalsResources()
        portalsAdapter = ResourceAdapter(portalsResources) { resource ->
            handleBookmark(resource)
        }
        binding.portalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = portalsAdapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                binding.clearSearchButton.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

                // Filter all adapters
                emergencyAdapter.filter(query)
                governmentAdapter.filter(query)
                legalAidAdapter.filter(query)
                helplinesAdapter.filter(query)
                portalsAdapter.filter(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }
    }

    private fun setupCategoryFilters() {
        binding.categoryChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val category = when (checkedId) {
                R.id.chip_all -> "All"
                R.id.chip_government -> "Government"
                R.id.chip_legal_aid -> "Legal Aid"
                R.id.chip_helplines -> "Helplines"
                R.id.chip_emergency -> "Emergency"
                else -> "All"
            }
            filterByCategory(category)
        }
    }

    private fun filterByCategory(category: String) {
        // Show/hide sections based on category
        when (category) {
            "All" -> {
                showAllSections()
            }
            "Emergency" -> {
                showOnlyEmergency()
            }
            "Government" -> {
                showOnlyGovernment()
            }
            "Legal Aid" -> {
                showOnlyLegalAid()
            }
            "Helplines" -> {
                showOnlyHelplines()
            }
        }
    }

    private fun showAllSections() {
        binding.emergencyRecyclerView.visibility = View.VISIBLE
        binding.governmentRecyclerView.visibility = View.VISIBLE
        binding.legalAidRecyclerView.visibility = View.VISIBLE
        binding.helplinesRecyclerView.visibility = View.VISIBLE
        binding.portalsRecyclerView.visibility = View.VISIBLE
    }

    private fun showOnlyEmergency() {
        binding.emergencyRecyclerView.visibility = View.VISIBLE
        binding.governmentRecyclerView.visibility = View.GONE
        binding.legalAidRecyclerView.visibility = View.GONE
        binding.helplinesRecyclerView.visibility = View.GONE
        binding.portalsRecyclerView.visibility = View.GONE
    }

    private fun showOnlyGovernment() {
        binding.emergencyRecyclerView.visibility = View.GONE
        binding.governmentRecyclerView.visibility = View.VISIBLE
        binding.legalAidRecyclerView.visibility = View.GONE
        binding.helplinesRecyclerView.visibility = View.GONE
        binding.portalsRecyclerView.visibility = View.VISIBLE
    }

    private fun showOnlyLegalAid() {
        binding.emergencyRecyclerView.visibility = View.GONE
        binding.governmentRecyclerView.visibility = View.GONE
        binding.legalAidRecyclerView.visibility = View.VISIBLE
        binding.helplinesRecyclerView.visibility = View.GONE
        binding.portalsRecyclerView.visibility = View.GONE
    }

    private fun showOnlyHelplines() {
        binding.emergencyRecyclerView.visibility = View.VISIBLE
        binding.governmentRecyclerView.visibility = View.GONE
        binding.legalAidRecyclerView.visibility = View.GONE
        binding.helplinesRecyclerView.visibility = View.VISIBLE
        binding.portalsRecyclerView.visibility = View.GONE
    }

    private fun handleBookmark(resource: Resource) {
        val message = if (resource.isBookmarked) {
            "${resource.title} bookmarked!"
        } else {
            "${resource.title} removed from bookmarks"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Data Sources

    private fun getEmergencyResources(): List<Resource> {
        return listOf(
            Resource(
                1,
                "Police Emergency",
                "Immediate police assistance for emergencies and crimes.",
                "Emergency",
                "100",
                null,
                R.drawable.ic_police
            ),
            Resource(
                2,
                "Women Helpline",
                "24x7 helpline for women in distress or danger.",
                "Emergency",
                "1091",
                "www.wcd.nic.in",
                R.drawable.ic_women
            ),
            Resource(
                3,
                "Child Helpline",
                "Free emergency phone service for children in need of aid and assistance.",
                "Emergency",
                "1098",
                "www.childlineindia.org",
                R.drawable.ic_child
            ),
            Resource(
                4,
                "Senior Citizen Helpline",
                "Helpline for elderly citizens facing any kind of abuse or problems.",
                "Emergency",
                "14567",
                null,
                R.drawable.ic_elderly
            )
        )
    }

    private fun getGovernmentResources(): List<Resource> {
        return listOf(
            Resource(
                10,
                "NALSA",
                "National Legal Services Authority provides free legal aid to eligible citizens including women, children, SC/ST, persons with disabilities, and those below poverty line.",
                "Government",
                "011-23388952",
                "nalsa.gov.in",
                R.drawable.ic_gavel
            ),
            Resource(
                11,
                "Ministry of Law & Justice",
                "Central government ministry responsible for administration of justice, legal reforms, and legislative affairs.",
                "Government",
                "011-23388952",
                "lawmin.gov.in",
                R.drawable.ic_government
            ),
            Resource(
                12,
                "State Legal Services Authority",
                "Your state-level authority for free legal aid services. Find your nearest District Legal Services Authority (DLSA) office.",
                "Government",
                null,
                "doj.gov.in/legal-aid",
                R.drawable.ic_law_book
            )
        )
    }

    private fun getLegalAidResources(): List<Resource> {
        return listOf(
            Resource(
                20,
                "Tele-Law",
                "Free legal consultation through video conferencing at Common Service Centers (CSCs) across India.",
                "Legal Aid",
                "155214",
                "tele-law.in",
                R.drawable.ic_video_call
            ),
            Resource(
                21,
                "Nyaya Bandhu",
                "Pro bono legal services through registered lawyers for marginalized communities.",
                "Legal Aid",
                null,
                "nyayabandhu.nic.in",
                R.drawable.ic_hands
            ),
            Resource(
                22,
                "Legal Aid Clinics",
                "Free legal advice clinics run by law schools and NGOs. Available in most major cities.",
                "Legal Aid",
                null,
                "nalsa.gov.in/services/legal-aid-clinics",
                R.drawable.ic_clinic
            ),
            Resource(
                23,
                "Lok Adalat",
                "Alternative dispute resolution forum for quick and amicable settlement of disputes.",
                "Legal Aid",
                null,
                "nalsa.gov.in/lok-adalat",
                R.drawable.ic_mediation
            )
        )
    }

    private fun getHelplinesResources(): List<Resource> {
        return listOf(
            Resource(
                30,
                "Legal Services Helpline",
                "Central helpline for legal aid information and guidance.",
                "Helplines",
                "15100",
                "doj.gov.in",
                R.drawable.ic_phone
            ),
            Resource(
                31,
                "Domestic Violence Helpline",
                "Support for victims of domestic violence and abuse.",
                "Helplines",
                "181",
                null,
                R.drawable.ic_help
            ),
            Resource(
                32,
                "Consumer Helpline",
                "For consumer complaints and grievances.",
                "Helplines",
                "1800-11-4000",
                "consumerhelpline.gov.in",
                R.drawable.ic_consumer
            ),
            Resource(
                33,
                "Cyber Crime Helpline",
                "Report cybercrimes and get assistance.",
                "Helplines",
                "1930",
                "cybercrime.gov.in",
                R.drawable.ic_cyber
            )
        )
    }

    private fun getPortalsResources(): List<Resource> {
        return listOf(
            Resource(
                40,
                "eCourts Services",
                "Access case status, cause lists, judgments, and court orders online.",
                "Government",
                null,
                "ecourts.gov.in",
                R.drawable.ic_court
            ),
            Resource(
                41,
                "Indian Kanoon",
                "Free access to Indian case law, statutes, and legal documents.",
                "Government",
                null,
                "indiankanoon.org",
                R.drawable.ic_search
            ),
            Resource(
                42,
                "PGPORTAL (Grievances)",
                "Public Grievance portal for lodging complaints against government departments.",
                "Government",
                null,
                "pgportal.gov.in",
                R.drawable.ic_complaint
            ),
            Resource(
                43,
                "National Commission for Women",
                "File complaints related to women's rights violations.",
                "Government",
                "011-26944880",
                "ncw.nic.in",
                R.drawable.ic_women
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}