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
    CKFinderUploadAdapter
} from 'ckeditor5';
import 'ckeditor5/ckeditor5.css';

import { getPostById, createPost, updatePost } from '../postApi';

const PostEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (id) {
            getPostById(id).then(data => {
                setTitle(data.title);
                setContent(data.content);
            }).catch(err => {
                console.error("데이터 로딩 실패", err);
                alert("게시글을 불러올 수 없습니다.");
            });
        }
    }, [id]);

    const handleSave = async () => {
        if (!title.trim() || !content.trim()) {
            alert("제목과 내용을 모두 입력해주세요.");
            return;
        }
        const postData = { title, content };
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

    return (
        <div className="container mt-5">
            <h2>{id ? '게시글 수정' : '새 게시글 작성'}</h2>
            <div className="card p-4 shadow-sm">
                <div className="mb-3">
                    <label className="form-label">제목</label>
                    <input
                        type="text"
                        className="form-control"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label">내용</label>
                    <CKEditor
                        editor={ClassicEditor}
                        data={content}
                        config={{
                            licenseKey: 'GPL',
                            plugins: [Essentials, Bold, Italic, Paragraph, Link, List, Image, ImageUpload, CKFinderUploadAdapter],
                            toolbar: ['undo', 'redo', '|', 'bold', 'italic', '|', 'link', 'bulletedList', 'numberedList', '|', 'imageUpload'],
                            ckfinder: {
                                uploadUrl: `${import.meta.env.VITE_API_BASE_URL}/api/image/upload`
                            },
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
                        {loading ? '저장 중...' : (id ? '수정하기' : '저장하기')}
                    </button>
                    <button onClick={() => navigate(-1)} className="btn btn-secondary">취소</button>
                </div>
            </div>
        </div>
    );
};

export default PostEditor;