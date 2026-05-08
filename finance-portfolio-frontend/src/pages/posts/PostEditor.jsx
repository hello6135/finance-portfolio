import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

import { CKEditor } from '@ckeditor/ckeditor5-react';
import {
    ClassicEditor,
    Bold,
    Italic,
    Essentials,
    Paragraph,
    Link,
    List,
    Image,
    ImageUpload,
} from 'ckeditor5';
import 'ckeditor5/ckeditor5.css';

import { getPostById, createPost, updatePost, imageUploadAdapter } from '../../api/postApi';
import { getCategories } from '../../api/categoryApi';

const PostEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(false);

    const [categories, setCategories] = useState([]); // 카테고리 목록
    const [categoryId, setCategoryId] = useState(''); // 선택된 카테고리 ID

    useEffect(() => {
        // 1. 카테고리 목록 로드
        getCategories().then(setCategories).catch(console.error);

        if (id) {
            getPostById(id).then(data => {
                setTitle(data.title);
                setContent(data.content);
                setCategoryId(data.categoryId ? String(data.categoryId) : '');
            }).catch(err => {
                console.error("데이터 로딩 실패", err);
                alert("게시글을 불러올 수 없습니다.");
            });
        }
    }, [id]);

    const handleSave = async () => {
        if (!title.trim() || !content.trim() || !categoryId) {
            alert("제목과 내용, 카테고리를 모두 입력해주세요.");
            return;
        }
        const postData = {
            title,
            content,
            categoryId: Number(categoryId)
        };
        setLoading(true);
        try {
            if (id) {
                await updatePost(id, postData);
                alert("수정되었습니다.");
            } else {
                await createPost(postData);
                alert("등록되었습니다.");
            }
            navigate('/posts');
        } catch (error) {
            console.error("저장 실패:", error);
            alert("저장 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };
    const getButtonText = () => {
        if (loading) return '저장 중...';
        return id ? '수정하기' : '저장하기';
    };

    return (
        <div className="container py-4">
            {/* 페이지 헤더 */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2 className="mb-0">{id ? '게시글 수정' : '새 게시글 작성'}</h2>
            </div>
            <div className="card p-4 shadow-sm">
                {/* 카테고리 선택 */}
                <div className="mb-3">
                    <label htmlFor="category" className="form-label">카테고리</label>
                    <select
                        id="category"
                        className="form-select"
                        value={String(categoryId)}
                        onChange={(e) => setCategoryId(e.target.value)}
                    >
                        <option value="">카테고리를 선택하세요</option>
                        {categories.map(category => (
                            <option key={category.id} value={String(category.id)}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="mb-3">
                    <label htmlFor="title" className="form-label">제목</label>
                    <input
                        id="title"
                        type="text"
                        className="form-control"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="content" className="form-label">내용</label>
                    <CKEditor
                        id="content"
                        className=".ck-editor__wrapper"
                        editor={ClassicEditor}
                        data={content}
                        onReady={(editor) => {
                            editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
                                return imageUploadAdapter(loader);
                            };
                        }}
                        config={{
                            licenseKey: 'GPL',
                            plugins: [Essentials, Bold, Italic, Paragraph, Link, List, Image, ImageUpload],
                            toolbar: ['undo', 'redo', '|', 'bold', 'italic', '|', 'link', 'bulletedList', 'numberedList', '|', 'imageUpload'],
                            placeholder: "내용을 입력하세요..."
                        }}
                        onChange={(event, editor) => {
                            const data = editor.getData();
                            setContent(data);
                        }}
                    />
                </div>

                <div className="d-flex gap-2">
                    <button onClick={handleSave} className="btn btn-primary" disabled={loading}>
                        {getButtonText()}
                    </button>
                    <button onClick={() => navigate(-1)} className="btn btn-secondary">취소</button>
                </div>
            </div>
        </div>
    );
};

export default PostEditor;