// Lost & Found AI Portal App State
const state = {
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    userName: localStorage.getItem('userName') || '',
    userRole: localStorage.getItem('userRole') || 'USER',
    activeMainTab: 'post',
    activeSearchMode: 'text',
    activeAuthTab: 'login',
    currentClaimPostId: null
};

// Elements
const authScreen = document.getElementById('auth-screen');
const dashboardScreen = document.getElementById('dashboard-screen');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const tabLoginBtn = document.getElementById('tab-login-btn');
const tabRegisterBtn = document.getElementById('tab-register-btn');
const userDisplayName = document.getElementById('user-display-name');
const userDisplayRole = document.getElementById('user-display-role');

const menuPostBtn = document.getElementById('menu-post-btn');
const menuSearchBtn = document.getElementById('menu-search-btn');
const tabPost = document.getElementById('tab-post');
const tabSearch = document.getElementById('tab-search');

const searchTextModeBtn = document.getElementById('search-text-mode-btn');
const searchImageModeBtn = document.getElementById('search-image-mode-btn');
const searchTextContainer = document.getElementById('search-text-container');
const searchImageContainer = document.getElementById('search-image-container');

const claimModal = document.getElementById('claim-modal');

// API Base Path (Serving from same host)
const API_BASE = '/api/v1';

// Authenticated fetch wrapper that handles silent token refresh
async function authenticatedFetch(url, options = {}) {
    options.headers = options.headers || {};
    if (state.token) {
        options.headers['Authorization'] = `Bearer ${state.token}`;
    }
    
    let response = await fetch(url, options);
    
    if (response.status === 401 && !options._isRetry) {
        options._isRetry = true;
        console.log('Access token expired. Attempting automatic refresh...');
        try {
            const refreshResponse = await fetch(`${API_BASE}/auth/refresh`, {
                method: 'POST'
            });
            
            if (refreshResponse.ok) {
                const refreshData = await refreshResponse.json();
                const dataObj = refreshData.data || refreshData;
                
                if (dataObj && dataObj.accessToken) {
                    state.token = dataObj.accessToken;
                    localStorage.setItem('token', dataObj.accessToken);
                    options.headers['Authorization'] = `Bearer ${state.token}`;
                    console.log('Token refreshed successfully. Retrying original request.');
                    return await fetch(url, options);
                }
            }
        } catch (refreshErr) {
            console.error('Failed to automatically refresh token:', refreshErr);
        }
        
        // If refresh fails or returns error, log out
        console.warn('Session expired. Logging out.');
        handleLogoutDirect();
    }
    return response;
}

function handleLogoutDirect() {
    state.token = '';
    state.userId = '';
    state.userName = '';
    state.userRole = 'USER';
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userName');
    localStorage.removeItem('userRole');
    showToast('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.', 'warning');
    showAuth();
}

// Initial Load Setup
window.addEventListener('DOMContentLoaded', () => {
    initApp();
    checkSystemHealth();
    // Periodically check health every 15 seconds
    setInterval(checkSystemHealth, 15000);
});

// Toast system
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span>${message}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// ----------------------------------------------------
// STATE & AUTHENTICATION FUNCTIONS
// ----------------------------------------------------
function initApp() {
    if (state.token) {
        showDashboard();
    } else {
        showAuth();
    }
}

function showAuth() {
    authScreen.classList.remove('hidden');
    dashboardScreen.classList.add('hidden');
}

function showDashboard() {
    authScreen.classList.add('hidden');
    dashboardScreen.classList.remove('hidden');
    userDisplayName.textContent = state.userName || 'Người dùng';
    userDisplayRole.textContent = state.userRole === 'ADMIN' ? 'Quản trị viên' : 'Thành viên';
    switchMainTab(state.activeMainTab);
}

function switchAuthTab(tab) {
    state.activeAuthTab = tab;
    if (tab === 'login') {
        tabLoginBtn.classList.add('active');
        tabRegisterBtn.classList.remove('active');
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
    } else {
        tabLoginBtn.classList.remove('active');
        tabRegisterBtn.classList.add('active');
        loginForm.classList.add('hidden');
        registerForm.classList.remove('hidden');
    }
}

async function handleLogin(event) {
    event.preventDefault();
    const mail = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mail, password })
        });

        if (!response.ok) {
            throw new Error('Email hoặc mật khẩu không chính xác');
        }

        const data = await response.json();
        saveAuthSession(data);
        const authData = data.data || data;
        showToast(`Chào mừng quay trở lại, ${authData.name}! 👋`, 'success');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const name = document.getElementById('register-name').value;
    const phone = document.getElementById('register-phone').value;
    const mail = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;

    try {
        const response = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, phone, mail, password })
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || 'Lỗi đăng ký tài khoản');
        }

        const data = await response.json();
        saveAuthSession(data);
        showToast('Đăng ký tài khoản thành công! 🎉', 'success');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function saveAuthSession(data) {
    const authData = data.data || data;
    state.token = authData.accessToken;
    state.userId = authData.userId;
    state.userName = authData.name;
    state.userRole = authData.userType || 'USER';

    localStorage.setItem('token', authData.accessToken);
    localStorage.setItem('userId', authData.userId);
    localStorage.setItem('userName', authData.name);
    localStorage.setItem('userRole', state.userRole);

    showDashboard();
}

function handleLogout() {
    // Call background logout endpoint if desired, but we can clear local state directly
    fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}` }
    }).catch(err => console.log("Logout backend request failed: ", err));

    state.token = '';
    state.userId = '';
    state.userName = '';
    state.userRole = 'USER';

    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userName');
    localStorage.removeItem('userRole');

    showToast('Đã đăng xuất', 'info');
    showAuth();
}

// ----------------------------------------------------
// SYSTEM HEALTH CHECK
// ----------------------------------------------------
async function checkSystemHealth() {
    try {
        const response = await fetch(`${API_BASE}/system/health`);
        const json = await response.json();
        const data = json.data || json;
        
        const clipInd = document.getElementById('status-clip');
        const ollamaInd = document.getElementById('status-ollama');

        if (data.clip === 'available' || data.clip === 'ok') {
            clipInd.textContent = 'ONLINE ✔';
            clipInd.className = 'status-indicator ok';
        } else {
            clipInd.textContent = 'LỖI ❌';
            clipInd.className = 'status-indicator error';
        }

        if (data.ollama === 'available' || data.ollama === 'ok') {
            ollamaInd.textContent = 'ONLINE ✔';
            ollamaInd.className = 'status-indicator ok';
        } else if (data.ollama === 'disabled') {
            ollamaInd.textContent = 'TẮT ⚠️';
            ollamaInd.className = 'status-indicator warning';
        } else {
            ollamaInd.textContent = 'LỖI ❌';
            ollamaInd.className = 'status-indicator error';
        }
    } catch (error) {
        console.error('Không thể check health hệ thống: ', error);
    }
}

// ----------------------------------------------------
// TAB NAVIGATION FUNCTIONS
// ----------------------------------------------------
function switchMainTab(tab) {
    state.activeMainTab = tab;
    if (tab === 'post') {
        menuPostBtn.classList.add('active');
        menuSearchBtn.classList.remove('active');
        tabPost.classList.remove('hidden');
        tabSearch.classList.add('hidden');
    } else {
        menuPostBtn.classList.remove('active');
        menuSearchBtn.classList.add('active');
        tabPost.classList.add('hidden');
        tabSearch.classList.remove('hidden');
    }
}

function switchSearchMode(mode) {
    state.activeSearchMode = mode;
    if (mode === 'text') {
        searchTextModeBtn.classList.add('active');
        searchImageModeBtn.classList.remove('active');
        searchTextContainer.classList.remove('hidden');
        searchImageContainer.classList.add('hidden');
    } else {
        searchTextModeBtn.classList.remove('active');
        searchImageModeBtn.classList.add('active');
        searchTextContainer.classList.add('hidden');
        searchImageContainer.classList.remove('hidden');
    }
}

function togglePostTypeWarning() {
    const postType = document.getElementById('post-type').value;
    const warningEl = document.getElementById('post-type-warning');
    if (postType === 'FOUND') {
        warningEl.classList.remove('hidden');
    } else {
        warningEl.classList.add('hidden');
    }
}

function previewImage(input, previewId) {
    const preview = document.getElementById(previewId);
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.src = e.target.result;
            preview.classList.remove('hidden');
        }
        reader.readAsDataURL(input.files[0]);
    } else {
        preview.src = '';
        preview.classList.add('hidden');
    }
}

// Helper to format ISO datetime for Spring Boot
function formatLocalDatetime(dtVal) {
    if (!dtVal) return null;
    // Spring accepts yyyy-MM-dd'T'HH:mm:ss
    return dtVal;
}

// ----------------------------------------------------
// CREATE POST SUBMISSION
// ----------------------------------------------------
async function handleCreatePost(event) {
    event.preventDefault();
    const submitBtn = document.getElementById('submit-post-btn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Đang xử lý đăng bài...';

    const postType = document.getElementById('post-type').value;
    const title = document.getElementById('post-title').value;
    const description = document.getElementById('post-description').value;
    const eventTimeVal = document.getElementById('post-event-time').value;

    const address = document.getElementById('post-address').value;
    const city = document.getElementById('post-city').value;
    const district = document.getElementById('post-district').value;
    const lat = document.getElementById('post-lat').value;
    const long = document.getElementById('post-long').value;

    const fileInput = document.getElementById('post-image');
    
    // Construct Form Data
    const formData = new FormData();
    formData.append('title', title);
    formData.append('description', description);
    if (eventTimeVal) formData.append('eventTime', formatLocalDatetime(eventTimeVal)); // null nếu để trống
    formData.append('userId', state.userId);
    formData.append('hidePostType', postType === 'FOUND' ? 'WHEN_MATCH' : 'PUBLIC');

    if (address) formData.append('address', address);
    if (city) formData.append('city', city);
    if (district) formData.append('district', district);
    if (lat) formData.append('latitude', lat);
    if (long) formData.append('longitude', long);
    if (address || city || district || lat || long) formData.append('locationLevel', 1);

    if (fileInput.files && fileInput.files[0]) {
        formData.append('image', fileInput.files[0]);
    }

    const endpoint = postType === 'FOUND' ? `${API_BASE}/posts/found` : `${API_BASE}/posts`;

    try {
        const response = await authenticatedFetch(endpoint, {
            method: 'POST',
            body: formData
        });

        let data = null;
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
            try {
                data = await response.json();
            } catch (jsonErr) {
                console.error("Failed to parse JSON: ", jsonErr);
            }
        }

        if (!response.ok) {
            const errMsg = (data && data.message) ? data.message : `Lỗi HTTP ${response.status}: ${response.statusText || 'Không xác định'}`;
            throw new Error(errMsg);
        }

        if (!data) {
            throw new Error("Phản hồi từ máy chủ không hợp lệ (không phải JSON)");
        }

        showToast('Đăng tin thành công! 🎉', 'success');
        
        // Reset Form
        document.getElementById('create-post-form').reset();
        document.getElementById('post-img-preview').classList.add('hidden');

        // Show immediate suggestions if matches returned
        const matchesContainer = document.getElementById('create-matches-container');
        const matchesSection = document.getElementById('match-results-on-create');
        
        matchesContainer.innerHTML = '';
        const postData = data.data || data;
        if (postData.matches && postData.matches.length > 0) {
            matchesSection.classList.remove('hidden');
            renderMatches(postData.matches, matchesContainer);
            showToast(`Hệ thống CLIP đã đối sánh chéo phát hiện ${postData.matches.length} tin khớp! Hãy cuộn xuống để xem.`, 'warning');
        } else {
            matchesSection.classList.add('hidden');
        }
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Đang bài ngay 🚀';
    }
}

// ----------------------------------------------------
// SEARCH FOR POSTS
// ----------------------------------------------------
async function handleSearch(event) {
    event.preventDefault();
    const resultsSection = document.getElementById('search-results-section');
    const resultsContainer = document.getElementById('search-results-container');
    const titleEl = document.getElementById('results-count-title');

    resultsSection.classList.add('hidden');
    resultsContainer.innerHTML = '';

    const targetType = document.getElementById('search-target-type').value;
    const topK = document.getElementById('search-top-k').value || 6;

    try {
        let response;
        if (state.activeSearchMode === 'text') {
            const textQuery = document.getElementById('search-text-input').value;
            if (!textQuery) throw new Error('Vui lòng nhập từ khóa tìm kiếm');
            
            response = await authenticatedFetch(`${API_BASE}/search/text`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    text: textQuery,
                    topK: parseInt(topK),
                    targetType: targetType
                })
            });
        } else {
            // Image search
            const fileInput = document.getElementById('search-image-input');
            const descExtra = document.getElementById('search-desc-extra').value;
            if (!fileInput.files || !fileInput.files[0]) {
                throw new Error('Vui lòng chọn một hình ảnh để tải lên đối sánh');
            }

            const formData = new FormData();
            formData.append('image', fileInput.files[0]);
            if (descExtra) {
                formData.append('description', descExtra);
            }
            
            // Append query parameters
            const url = new URL(`${window.location.origin}${API_BASE}/search`);
            url.searchParams.append('top_k', topK);
            url.searchParams.append('target_type', targetType);

            response = await authenticatedFetch(url.toString(), {
                method: 'POST',
                body: formData
            });
        }

        if (!response.ok) {
            throw new Error('Lỗi truy vấn tìm kiếm đồ vật');
        }

        const data = await response.json();
        const searchData = data.data || data;
        titleEl.textContent = `Kết quả tìm thấy (${searchData.total || 0})`;
        resultsSection.classList.remove('hidden');

        if (searchData.results && searchData.results.length > 0) {
            renderSearchResults(searchData.results, resultsContainer);
        } else {
            resultsContainer.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px 0;">Không tìm thấy tin trùng khớp nào phù hợp với từ khóa/ảnh của bạn.</p>';
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ----------------------------------------------------
// RENDERING FUNCTIONS
// ----------------------------------------------------
function renderSearchResults(results, container) {
    results.forEach(post => {
        const card = document.createElement('div');
        card.className = 'result-card glass';

        const isLost = post.type === 'LOST';
        const formattedDate = post.event_time ? new Date(post.event_time).toLocaleString('vi-VN') : 'Không rõ thời gian';
        const scorePercent = post.match_score ? Math.round(post.match_score * 100) : null;
        
        let imgHtml = '';
        if (post.blurred_image_url) {
            imgHtml = `<img class="result-img" src="${post.blurred_image_url}" alt="${post.title}">`;
        } else {
            imgHtml = `<div class="result-img" style="background: rgba(0,0,0,0.4); display:flex; align-items:center; justify-content:center;">🖼️ Không có ảnh</div>`;
        }

        card.innerHTML = `
            <div class="result-img-wrapper">
                ${imgHtml}
                ${scorePercent ? `<span class="score-tag">Độ khớp: ${scorePercent}%</span>` : ''}
                <span class="type-tag ${isLost ? 'lost' : 'found'}">${isLost ? 'Mất đồ' : 'Nhặt được'}</span>
            </div>
            <div class="result-content">
                <h4>${post.title}</h4>
                <p class="result-desc">${post.description || 'Không có mô tả chi tiết.'}</p>
                <div class="result-meta">
                    <span>📍 <strong>Địa điểm:</strong> ${post.location && post.location.address ? `${post.location.address}, ${post.location.district}, ${post.location.city}` : 'Không rõ địa chỉ'}</span>
                    <span>📅 <strong>Thời gian:</strong> ${formattedDate}</span>
                </div>
                ${!isLost ? `
                    <button class="btn-primary ripple-btn" onclick="openClaimModal(${post.post_id}, '${post.blurred_image_url}')" style="margin-top: auto; padding: 10px 15px; font-size: 0.9rem;">
                        Nhận lại đồ (Claim) 🛡️
                    </button>
                ` : ''}
            </div>
        `;
        container.appendChild(card);
    });
}

// Match suggestions rendering (on post creation)
async function renderMatches(matches, container) {
    // ClipMatch only has postId and score. We need details!
    // Since we don't have a direct bulk get post, let's fetch details of each matched post.
    for (const match of matches) {
        try {
            // Get questions is open, but to see metadata we can just query using post status or look at our search code logic.
            // Wait, we can fetch the verification questions.
            const card = document.createElement('div');
            card.className = 'result-card glass';

            const scorePercent = match.score ? Math.round(match.score * 100) : 0;
            
            card.innerHTML = `
                <div class="result-img-wrapper" style="height: 120px;">
                    <div class="result-img" style="background: rgba(0,0,0,0.5); display:flex; align-items:center; justify-content:center; font-size:2rem;">🎁</div>
                    <span class="score-tag">Khớp: ${scorePercent}%</span>
                </div>
                <div class="result-content" style="padding: 15px;">
                    <h4>Post ID: #${match.postId}</h4>
                    <p class="result-desc" style="-webkit-line-clamp: 2;">Điểm số tương đồng CLIP cao. Vui lòng vào Tab 'Tìm kiếm & Claim' để thực hiện nhận lại đồ bảo mật.</p>
                </div>
            `;
            container.appendChild(card);
        } catch (e) {
            console.error(e);
        }
    }
}

// ----------------------------------------------------
// CLAIM & AI VERIFICATION FLOW
// ----------------------------------------------------
async function openClaimModal(postId, blurredUrl) {
    state.currentClaimPostId = postId;
    const modalBlurredImg = document.getElementById('modal-blurred-img');
    const questionsContainer = document.getElementById('dynamic-questions-container');
    const resultBox = document.getElementById('claim-result');

    resultBox.classList.add('hidden');
    resultBox.innerHTML = '';
    questionsContainer.innerHTML = 'Đang tải bộ câu hỏi xác minh do AI sinh... 🚀';

    modalBlurredImg.src = blurredUrl || '';
    claimModal.classList.remove('hidden');

    try {
        const response = await authenticatedFetch(`${API_BASE}/posts/${postId}/verifications`);

        if (!response.ok) {
            throw new Error('Lỗi lấy câu hỏi xác minh');
        }

        const data = await response.json();
        const verificationData = data.data || data;
        
        if (verificationData.status === 'NOT_GENERATED' || !verificationData.questions || verificationData.questions.length === 0) {
            questionsContainer.innerHTML = `
                <div style="text-align: center; padding: 20px; color: var(--warning);">
                    ⚠️ AI chưa kịp sinh câu hỏi xác minh cho bài này (hoặc bài đăng không có ảnh). <br>
                    <button class="btn-secondary" onclick="openClaimModal(${postId}, '${blurredUrl}')" style="margin-top: 15px; padding: 6px 12px; font-size: 0.8rem;">
                        Thử lại sau 🔄
                    </button>
                </div>`;
            return;
        }

        renderQuestions(verificationData.questions, questionsContainer);

    } catch (error) {
        questionsContainer.innerHTML = `<p style="color: var(--danger); text-align: center;">Lỗi: ${error.message}</p>`;
    }
}

function closeClaimModal() {
    claimModal.classList.add('hidden');
    state.currentClaimPostId = null;
}

function renderQuestions(questions, container) {
    container.innerHTML = '';

    questions.forEach((q, idx) => {
        const box = document.createElement('div');
        box.className = 'dynamic-question-box';
        
        const priorityStars = '⭐'.repeat(q.important_point || 1);
        
        let inputHtml = '';
        if (q.type === 'BOOLEAN') {
            inputHtml = `
                <div class="question-input-wrapper" style="display:flex; gap: 20px;">
                    <label class="mcq-option" style="flex:1; margin-bottom: 0;">
                        <input type="radio" name="question-${q.id}" value="có" required> Có / Đúng
                    </label>
                    <label class="mcq-option" style="flex:1; margin-bottom: 0;">
                        <input type="radio" name="question-${q.id}" value="không" required> Không / Sai
                    </label>
                </div>
            `;
        } else if (q.type === 'MULTIPLE_CHOICE') {
            const options = q.options || [];
            inputHtml = `<div class="question-input-wrapper">`;
            options.forEach(opt => {
                inputHtml += `
                    <label class="mcq-option">
                        <input type="radio" name="question-${q.id}" value="${opt}" required> ${opt}
                    </label>
                `;
            });
            inputHtml += `</div>`;
        } else {
            // TEXT
            inputHtml = `
                <div class="question-input-wrapper">
                    <input type="text" id="question-${q.id}" class="text-answer-input" required placeholder="Nhập câu trả lời ngắn của bạn..." style="width:100%;">
                </div>
            `;
        }

        box.innerHTML = `
            <div class="question-text">
                <span><strong>Câu ${idx + 1}:</strong> ${q.question}</span>
                <span style="font-size: 0.7rem;">${priorityStars}</span>
            </div>
            ${q.hint ? `<p style="font-size: 0.75rem; color: var(--text-muted); margin-bottom: 8px;">💡 Gợi ý: ${q.hint}</p>` : ''}
            ${inputHtml}
        `;
        container.appendChild(box);
    });
}

async function submitClaim(event) {
    event.preventDefault();
    const resultBox = document.getElementById('claim-result');
    resultBox.classList.add('hidden');
    resultBox.innerHTML = 'Đang gửi câu trả lời và chấm điểm... 🔐';

    const questionsContainer = document.getElementById('dynamic-questions-container');
    const inputs = questionsContainer.querySelectorAll('.dynamic-question-box');
    
    const answers = [];

    // Parse answers
    inputs.forEach(box => {
        // Find questionId by looking at name of radio or id of text input
        const textInput = box.querySelector('input[type="text"]');
        if (textInput) {
            const qId = textInput.id.replace('question-', '');
            answers.push({
                question_id: parseInt(qId),
                answer: textInput.value
            });
        } else {
            const radio = box.querySelector('input[type="radio"]:checked');
            if (radio) {
                const qId = radio.name.replace('question-', '');
                answers.push({
                    question_id: parseInt(qId),
                    answer: radio.value
                });
            }
        }
    });

    try {
        const response = await authenticatedFetch(`${API_BASE}/posts/${state.currentClaimPostId}/claim`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ answers })
        });

        const data = await response.json();
        // Unwrap ApiResponse envelope (data.data) with fallback for legacy format
        const claimData = data.data || data;
        
        resultBox.className = 'claim-result-box';
        resultBox.classList.remove('hidden');

        if (response.ok && claimData.approved) {
            resultBox.classList.add('success');
            
            const owner = claimData.details?.owner || {};
            const scorePercent = Math.round(claimData.score * 100);

            resultBox.innerHTML = `
                <div class="success-unlocked-card">
                    <h4>🎉 Xác Minh Thành Công! (${scorePercent}%)</h4>
                    <p style="font-size: 0.85rem; margin-top: 5px;">Hệ thống đã phê duyệt và mở khóa thông tin liên hệ của tin đăng này.</p>
                    
                    ${claimData.details?.image_url ? `<img src="${claimData.details.image_url}" alt="Unlocked Clear Image" style="width:100%;border-radius:8px;margin:10px 0;">` : ''}
                    
                    <div class="owner-info-box">
                        <h5>Thông Tin Liên Hệ Nhận Đồ</h5>
                        <div class="owner-info-item"><strong>Người nhặt:</strong> <span>${owner.full_name || 'N/A'}</span></div>
                        <div class="owner-info-item"><strong>Số điện thoại:</strong> <span>${owner.phone || 'N/A'}</span></div>
                        <div class="owner-info-item"><strong>Email:</strong> <span>${owner.email || 'N/A'}</span></div>
                        <div class="owner-info-item"><strong>Tin đăng số:</strong> <span>#${claimData.details?.post_id}</span></div>
                    </div>
                </div>
            `;
            showToast('Xác minh thành công! Đã mở khóa thông tin đồ vật.', 'success');
        } else {
            // Rejected (403) - claimData chứa ClaimResponse
            resultBox.classList.add('error');
            const scorePercent = Math.round((claimData.score || 0) * 100);
            resultBox.innerHTML = `
                <h4>❌ Xác Minh Thất Bại</h4>
                <p style="font-size: 0.9rem; margin-top: 5px;">${claimData.message || 'Mức độ khớp câu trả lời chưa đạt yêu cầu.'}</p>
                <div style="margin-top: 10px; font-weight: 500;">
                    Điểm số của bạn: <span style="color: var(--danger); font-size: 1.1rem;">${scorePercent}%</span> 
                    (Yêu cầu tối thiểu: ${Math.round((claimData.threshold || 0.6) * 100)}%)
                </div>
            `;
            showToast('Xác minh thất bại. Điểm số chưa đạt 60%', 'error');
        }

    } catch (error) {
        resultBox.className = 'claim-result-box error';
        resultBox.classList.remove('hidden');
        resultBox.textContent = `Lỗi hệ thống: ${error.message}`;
    }
}
