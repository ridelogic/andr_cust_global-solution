package com.arwe.newproject.ui.booking

import com.arwe.newproject.R

// Static placeholder catalog. Replace with a repository/API call once the
// backend problem taxonomy exists. `product` is accepted so the list can be
// differentiated per product later; only `category` drives it for now.
object ProblemCatalog {

    fun problemsFor(category: ServiceCategory, product: Product): List<Problem> = when (category) {
        ServiceCategory.BATTERY -> listOf(
            Problem(
                id = "not_charging",
                titleRes = R.string.problem_not_charging_title,
                descriptionRes = R.string.problem_not_charging_description,
                iconRes = R.drawable.ic_problem_not_charging,
                complaintValue = "Not Charging"
            ),
            Problem(
                id = "low_backup",
                titleRes = R.string.problem_low_backup_title,
                descriptionRes = R.string.problem_low_backup_description,
                iconRes = R.drawable.ic_problem_low_backup,
                complaintValue = "Low Backup"
            ),
            Problem(
                id = "dead_battery",
                titleRes = R.string.problem_dead_battery_title,
                descriptionRes = R.string.problem_dead_battery_description,
                iconRes = R.drawable.ic_problem_not_working,
                complaintValue = "Dead Battery"
            ),
            Problem(
                id = "regeneration_required",
                titleRes = R.string.problem_regeneration_required_title,
                descriptionRes = R.string.problem_regeneration_required_description,
                iconRes = R.drawable.ic_problem_regeneration,
                complaintValue = "Regeneration Required"
            ),
            Problem(
                id = "other",
                titleRes = R.string.problem_other_title,
                descriptionRes = R.string.problem_other_description,
                iconRes = R.drawable.ic_category_other,
                complaintValue = "Other"
            )
        )

        else -> listOf(
            Problem(
                id = "not_working",
                titleRes = R.string.problem_generic_not_working_title,
                descriptionRes = R.string.problem_generic_not_working_description,
                iconRes = R.drawable.ic_problem_not_working,
                complaintValue = "Not Working"
            ),
            Problem(
                id = "other",
                titleRes = R.string.problem_generic_other_title,
                descriptionRes = R.string.problem_generic_other_description,
                iconRes = R.drawable.ic_category_other,
                complaintValue = "Other"
            )
        )
    }
}
