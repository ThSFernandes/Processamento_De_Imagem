import tkinter as tk
from tkinter import filedialog
from PIL import Image, ImageTk
import cv2
import numpy as np

# Função para abrir a imagem
def open_image():
    file_path = filedialog.askopenfilename()
    if file_path:
        global img_cv
        img_cv = cv2.imread(file_path, cv2.IMREAD_GRAYSCALE)
        

        if img_cv is None:
            print('Não foi possível abrir ou encontrar a imagem.')
            return
        
        img = Image.fromarray(img_cv)
        img = img.resize((200, 200))  # Redimensiona a imagem para caber nas janelas
        
        # Converte a imagem para um formato que o tkinter possa usar
        img_tk = ImageTk.PhotoImage(img)
        
        # Exibe a imagem no label original
        label_original.config(image=img_tk)
        label_original.image = img_tk
        
        # Armazena o caminho da imagem para aplicar o filtro
        label_original.image_path = file_path

# Função auxiliar para aplicar a Transformada de Fourier e filtro
def apply_fourier_filter(filter_type):
    if hasattr(label_original, 'image_path'):
        img = img_cv

        # Aplicar a Transformada de Fourier
        dft = cv2.dft(np.float32(img), flags=cv2.DFT_COMPLEX_OUTPUT)
        dft_shift = np.fft.fftshift(dft)

        # Obter as dimensões da imagem
        rows, cols = img.shape
        crow, ccol = rows // 2, cols // 2  # Centro da imagem

        # Criar uma máscara (passa-baixa ou passa-alta)
        mask = np.zeros((rows, cols, 2), np.uint8)
        if filter_type == 'low_pass':
            mask[crow-30:crow+30, ccol-30:ccol+30] = 1
            filtered_img = dft_shift * mask
        elif filter_type == 'high_pass':
            mask[:, :] = 1
            mask[crow-30:crow+30, ccol-30:ccol+30] = 0
            filtered_img = dft_shift * mask

        # Aplicar a máscara e a transformada inversa
        f_ishift = np.fft.ifftshift(filtered_img)
        img_back = cv2.idft(f_ishift)
        img_back = cv2.magnitude(img_back[:,:,0], img_back[:,:,1])

        # Normalizar os valores para a faixa de 0 a 255
        cv2.normalize(img_back, img_back, 0, 255, cv2.NORM_MINMAX)
        img_back = np.uint8(img_back)

        # Converte a imagem filtrada para um formato que o tkinter possa usar
        img_back = Image.fromarray(img_back)
        img_back = img_back.resize((200, 200))
        img_tk = ImageTk.PhotoImage(img_back)

        # Atualiza o label correspondente ao filtro aplicado
        if filter_type == 'low_pass':
            label_low_pass.config(image=img_tk)
            label_low_pass.image = img_tk
        elif filter_type == 'high_pass':
            label_high_pass.config(image=img_tk)
            label_high_pass.image = img_tk

# Função para aplicar erosão
def apply_erosion():
    if hasattr(label_original, 'image_path'):
        global img_cv
        erosion_size = erosion_size_var.get()
        erosion_shape = morph_shape(erosion_shape_var.get())

        element = cv2.getStructuringElement(erosion_shape, (2 * erosion_size + 1, 2 * erosion_size + 1),
                                           (erosion_size, erosion_size))
        
        erosion_dst = cv2.erode(img_cv, element)
        img = Image.fromarray(erosion_dst)
        img = img.resize((200, 200))
        img_tk = ImageTk.PhotoImage(img)

        label_erosion.config(image=img_tk)
        label_erosion.image = img_tk

# Função para aplicar dilatação
def apply_dilation():
    if hasattr(label_original, 'image_path'):
        global img_cv
        dilation_size = dilation_size_var.get()
        dilation_shape = morph_shape(dilation_shape_var.get())
        
        element = cv2.getStructuringElement(dilation_shape, (2 * dilation_size + 1, 2 * dilation_size + 1),
                                           (dilation_size, dilation_size))
        
        dilatation_dst = cv2.dilate(img_cv, element)
        img = Image.fromarray(dilatation_dst)
        img = img.resize((200, 200))
        img_tk = ImageTk.PhotoImage(img)

        label_dilation.config(image=img_tk)
        label_dilation.image = img_tk

# Função auxiliar para obter o tipo de elemento estruturante
def morph_shape(val):
    if val == 0:
        return cv2.MORPH_RECT
    elif val == 1:
        return cv2.MORPH_CROSS
    elif val == 2:
        return cv2.MORPH_ELLIPSE

# Cria a janela principal
root = tk.Tk()
root.geometry("1600x600")
root.title("Visualizador de Imagens com Filtragem")

# Cria um frame para a imagem original
frame_original = tk.Frame(root)
frame_original.pack(side=tk.LEFT, padx=50)

# Cria um frame para a imagem com filtro passa-baixa
frame_low_pass = tk.Frame(root)
frame_low_pass.pack(side=tk.LEFT, padx=10)

# Cria um frame para a imagem com filtro passa-alta
frame_high_pass = tk.Frame(root)
frame_high_pass.pack(side=tk.LEFT, padx=10)

# Cria um frame para a imagem com erosão
frame_erosion = tk.Frame(root)
frame_erosion.pack(side=tk.LEFT, padx=10)

# Cria um frame para a imagem com dilatação
frame_dilation = tk.Frame(root)
frame_dilation.pack(side=tk.LEFT, padx=10)

# Cria um botão para abrir a imagem
button_open = tk.Button(root, text="Abrir Imagem", command=open_image)
button_open.pack(pady=10)

# Cria botões para aplicar os filtros
button_low_pass = tk.Button(root, text="Aplicar Filtro Passa-Baixa", command=lambda: apply_fourier_filter('low_pass'))
button_low_pass.pack(pady=5)

button_high_pass = tk.Button(root, text="Aplicar Filtro Passa-Alta", command=lambda: apply_fourier_filter('high_pass'))
button_high_pass.pack(pady=5)

button_erosion = tk.Button(root, text="Aplicar Erosão", command=apply_erosion)
button_erosion.pack(pady=5)

button_dilation = tk.Button(root, text="Aplicar Dilatação", command=apply_dilation)
button_dilation.pack(pady=5)

# Cria labels para exibir as imagens
label_original = tk.Label(frame_original, text="Imagem Original")
label_original.pack()

label_low_pass = tk.Label(frame_low_pass, text="Filtro Passa-Baixa")
label_low_pass.pack()

label_high_pass = tk.Label(frame_high_pass, text="Filtro Passa-Alta")
label_high_pass.pack()

label_erosion = tk.Label(frame_erosion, text="Erosão")
label_erosion.pack()

label_dilation = tk.Label(frame_dilation, text="Dilatação")
label_dilation.pack()

# Trackbars (sliders) para parâmetros de erosão
erosion_size_var = tk.IntVar(value=1)
erosion_shape_var = tk.IntVar(value=0)  # Default: Rect

tk.Label(root, text="Tamanho do Kernel de Erosão").pack()
tk.Scale(root, from_=1, to_=20, orient=tk.HORIZONTAL, variable=erosion_size_var).pack()

tk.Label(root, text="Forma do Elemento Estruturante de Erosão").pack()
tk.Scale(root, from_=0, to_=2, orient=tk.HORIZONTAL, variable=erosion_shape_var, 
         label="0: Retângulo, 1: Cruz, 2: Elipse").pack()

# Trackbars (sliders) para parâmetros de dilatação
dilation_size_var = tk.IntVar(value=1)
dilation_shape_var = tk.IntVar(value=0)  # Default: Rect

tk.Label(root, text="Tamanho do Kernel de Dilatação").pack()
tk.Scale(root, from_=1, to_=2, orient=tk.HORIZONTAL, variable=dilation_size_var).pack()

tk.Label(root, text="Forma do Elemento Estruturante de Dilatação").pack()
tk.Scale(root, from_=0, to_=2, orient=tk.HORIZONTAL, variable=dilation_shape_var,
         label="0: Retângulo, 1: Cruz, 2: Elipse").pack()

# Inicia o loop principal do tkinter
root.mainloop()
