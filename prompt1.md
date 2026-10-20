You are an expert software engineer and code parser specializing in data structure analysis and sorting algorithms. 

I will provide you with a source code file (which includes sorting logic, potentially inside a `qsort` implementation, comparator functions, or data structures). Your task is to analyze the file and extract the complete sorting hierarchy for every field.

### Instructions:
1. Identify all fields referenced in the sorting logic, comparator routines, or `qsort` comparison functions.
2. Determine the sorting precedence for each field:
   - **Primary Sorting:** The main field used as the first condition to order the data.
   - **Secondary Sorting:** The subsequent field(s) used as tie-breakers when primary values are equal.
   - **Tertiary/Higher Levels:** Any further fallback sorting criteria, if present.
3. Identify the sort direction for each field (e.g., Ascending, Descending, or conditional).
4. Note the specific context or function name where this sorting rule is defined (e.g., specific `qsort` callback, SQL order clause, or custom sort routine).

### Output Format:
Present your findings in a clear, organized Markdown table with the following columns:
- **Field Name**
- **Sort Level** (Primary, Secondary, etc.)
- **Sort Direction** (Ascending / Descending)
- **Context / Function** (e.g., `qsort_comparator_func`)
- **Notes / Logic Description** (Brief explanation of how the comparison is structured)

Here is the file content:
[PASTE YOUR CODE / FILE CONTENT HERE]
