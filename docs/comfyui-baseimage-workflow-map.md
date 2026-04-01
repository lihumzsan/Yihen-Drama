# ComfyUI Baseimage Workflow Map

This project uses the grouped workflows under `D:\comfui\workflows\baseimage`.

## Active Image Workflows

- `CHARACTER_GEN`
  - Primary: `D:\comfui\workflows\baseimage\图片生成\zimage\场景文生图-造相ZImageTurbo官方版-RunningHub原始工作流.json`
  - Regenerate: `D:\comfui\workflows\baseimage\图片编辑\qwen\单图编辑.json`
- `SCENE_GEN`
  - Primary: `D:\comfui\workflows\baseimage\图片生成\zimage\场景文生图-造相ZImageTurbo官方版-RunningHub原始工作流.json`
- `FIRST_FRAME_GEN`
  - Primary: `D:\comfui\workflows\baseimage\图片编辑\flux\参考编辑-5图编辑创作Flux2多图编辑-RunningHub原始工作流.json`

## Node Mapping

- `image-generate / zimage / official`
  - Output node: `10`
- `image-edit / qwen / single-edit`
  - Prompt node: `44`
  - Image node: `49`
  - Output node: `46`
- `image-edit / flux / five-image-edit`
  - Prompt node: `25`
  - Image nodes: `22, 23, 21, 27, 26`
  - Output node: `35`
