const titleInput = document.getElementById('title');
const authorInput = document.getElementById('author');
const priceInput = document.getElementById('price');
const quantityInput = document.getElementById('quantity');
const booksBody = document.getElementById('books-body');

let selectedId = null;

window.addEventListener('DOMContentLoaded', () => {
    document.getElementById('add-btn').addEventListener('click', addBook);
    document.getElementById('update-btn').addEventListener('click', updateBook);
    document.getElementById('delete-btn').addEventListener('click', deleteBook);
    fetchBooks();
});

async function fetchBooks() {
    const response = await fetch('/api/books');
    const books = await response.json();
    renderBooks(books);
}

function renderBooks(books) {
    booksBody.innerHTML = books.map(book => `
        <tr data-id="${book.id}">
            <td>${escapeHtml(book.title)}</td>
            <td>${escapeHtml(book.author)}</td>
            <td>$${book.price.toFixed(2)}</td>
            <td>${book.quantity}</td>
        </tr>
    `).join('');

    booksBody.querySelectorAll('tr').forEach(row => {
        row.addEventListener('click', () => {
            booksBody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
            row.classList.add('selected');
            const id = Number(row.dataset.id);
            const book = books.find(item => item.id === id);
            selectBook(book);
        });
    });
}

function selectBook(book) {
    selectedId = book.id;
    titleInput.value = book.title;
    authorInput.value = book.author;
    priceInput.value = book.price;
    quantityInput.value = book.quantity;
}

function clearForm() {
    selectedId = null;
    titleInput.value = '';
    authorInput.value = '';
    priceInput.value = '';
    quantityInput.value = '';
    booksBody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
}

function getFormData() {
    return new URLSearchParams({
        title: titleInput.value.trim(),
        author: authorInput.value.trim(),
        price: priceInput.value.trim(),
        quantity: quantityInput.value.trim(),
    });
}

function validateForm() {
    if (!titleInput.value.trim() || !authorInput.value.trim() || !priceInput.value.trim() || !quantityInput.value.trim()) {
        alert('Fill in all fields before saving.');
        return false;
    }
    if (Number.isNaN(Number(priceInput.value)) || Number(priceInput.value) < 0) {
        alert('Enter a valid price.');
        return false;
    }
    if (!Number.isInteger(Number(quantityInput.value)) || Number(quantityInput.value) < 0) {
        alert('Enter a valid quantity.');
        return false;
    }
    return true;
}

async function addBook() {
    if (!validateForm()) {
        return;
    }
    await fetch('/api/books', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body: getFormData().toString(),
    });
    clearForm();
    fetchBooks();
}

async function updateBook() {
    if (!selectedId) {
        alert('Select a book from the table first.');
        return;
    }
    if (!validateForm()) {
        return;
    }
    await fetch(`/api/books?id=${selectedId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body: getFormData().toString(),
    });
    clearForm();
    fetchBooks();
}

async function deleteBook() {
    if (!selectedId) {
        alert('Select a book from the table first.');
        return;
    }
    if (!confirm('Delete the selected book?')) {
        return;
    }
    await fetch(`/api/books?id=${selectedId}`, {
        method: 'DELETE'
    });
    clearForm();
    fetchBooks();
}

function escapeHtml(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
