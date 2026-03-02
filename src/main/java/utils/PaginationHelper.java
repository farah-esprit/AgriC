package com.eventmanagement.utils;

public class PaginationHelper {
    
    private int totalItems;
    private int pageSize;
    private int currentPage;
    private int totalPages;
    
    public PaginationHelper(int totalItems, int pageSize) {
        this.totalItems = totalItems;
        this.pageSize = pageSize;
        this.currentPage = 1;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int pageNum) {
        if (pageNum >= 1 && pageNum <= totalPages) {
            this.currentPage = pageNum;
        }
    }
    
    public boolean hasNextPage() {
        return currentPage < totalPages;
    }
    
    public boolean hasPreviousPage() {
        return currentPage > 1;
    }
    
    public void nextPage() {
        if (hasNextPage()) {
            currentPage++;
        }
    }
    
    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
        }
    }
    
    public int getOffset() {
        return (currentPage - 1) * pageSize;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    
    public String getPageInfo() {
        return String.format("Page %d / %d (Total: %d items)", currentPage, totalPages, totalItems);
    }
}
