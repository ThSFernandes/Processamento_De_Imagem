/*
* Prática de Processamento Digital de Imagens
* prof.  ngelo Magno de Jesus
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

@SuppressWarnings("serial")
public class ImageIFMG extends JFrame {

    private class ProvedorImagens {

        private static final List<ProvedorImagens> provedoresImagens = new ArrayList<>();
        private final List<BufferedImage> imagens;
        private final Object chaveEspera;
        private final int qtdImagensEsperando;

        public ProvedorImagens obterProvedor(Object chaveEspera, int qtdImagensEsperando) {

            ProvedorImagens provedorImagens = new ProvedorImagens(chaveEspera, qtdImagensEsperando);
            provedoresImagens.add(provedorImagens);
            return provedorImagens;
        }

        public BufferedImage get(int index) {
            return imagens.get(index);
        }

        public void atualizarProvedores(BufferedImage imagem) {
            provedoresImagens.forEach(provedor -> provedor.addImagem(imagem));
        }

        private ProvedorImagens(Object chaveEspera, int qtdImagensEsperando) {
            imagens = new ArrayList<>();
            this.chaveEspera = chaveEspera;
            this.qtdImagensEsperando = qtdImagensEsperando;
        }

        private void addImagem(BufferedImage imagem) {
            if (imagens.contains(imagem)) {
                return;
            }

            imagens.add(imagem);
            synchronized (chaveEspera) {
                if (imagens.size() == qtdImagensEsperando) {
                    provedoresImagens.remove(this);
                    chaveEspera.notifyAll();
                }
            }
        }
    }

    private JDesktopPane jDesktopPaneImagem;
    private JMenuBar jMenuBar;
    private JMenu jMenuAbrir;
    private JMenu jMenuProcessar;
    private JMenu jMenuSalvar;
    private JMenuItem jMenuItemAbrirImagem;
    private JMenuItem jMenuItemCriarInternalFrame;
    private JMenuItem[] jMenuItemsProcessar;
    private JMenuItem jMenuItemSalvar;
    private BufferedImage imagemSelecionada;
    private JInternalFrame frameAtual;
    private List<int[][]> matrizRGB;
    private List<BufferedImage> imagens = new ArrayList<>();
    private static final int ALTURA_JANELA = 400;
    private static final int LARGURA_JANELA = 600;
    private static final int DIFERENCA_TOTAL_MAXIMA = 30;
    private static final int BRANCO = 255;
    private static final int PRETO = 0;

    public void escalaCinza(List<int[][]> matrizRGB) {
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;
        int[][] matrizRed = matrizRGB.get(0);
        int[][] matrizGreen = matrizRGB.get(1);
        int[][] matrizBlue = matrizRGB.get(2);
        int[][] matrizMedia = new int[altura][largura];

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                matrizMedia[i][j] = (matrizRed[i][j] + matrizGreen[i][j] + matrizBlue[i][j]) / 3;
            }
        }
        gerarImagem(matrizMedia, matrizMedia, matrizMedia);
    }

    public void imagemBinaria(List<int[][]> matrizRGB) {
        int[][] matrizBinaria = obterImagemBinaria(matrizRGB);

        gerarImagem(matrizBinaria, matrizBinaria, matrizBinaria);
    }

    private int[][] obterImagemBinaria(List<int[][]> matrizRGB) {
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;

        int[][] matrizRed = matrizRGB.get(0);
        int[][] matrizGreen = matrizRGB.get(1);
        int[][] matrizBlue = matrizRGB.get(2);

        int[][] matrizBinaria = new int[altura][largura];
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                int media = (matrizRed[i][j] + matrizGreen[i][j] + matrizBlue[i][j]) / 3;
                matrizBinaria[i][j] = media > 127 ? 255 : 0;
            }
        }

        return matrizBinaria;
    }

    public void imagemNegativa(List<int[][]> matrizRGB) {
        List<int[][]> novaMatrizRGB = new LinkedList<>();
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;
        for (int i = 0; i < 3; i++) {
            int[][] matriz = matrizRGB.get(i);
            int[][] novaMatriz = new int[altura][largura];
            for (int j = 0; j < altura; j++) {
                for (int k = 0; k < largura; k++) {
                    novaMatriz[j][k] = 255 - matriz[j][k];
                }
            }
            novaMatrizRGB.add(novaMatriz);
        }
        gerarImagem(novaMatrizRGB.get(0), novaMatrizRGB.get(1), novaMatrizRGB.get(2));
    }

    public void corDominante(List<int[][]> matrizRGB) {
        List<int[][]> novaMatrizRGB = new LinkedList<>();
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;
        int[][] matrizRed = matrizRGB.get(0);
        int[][] matrizGreen = matrizRGB.get(1);
        int[][] matrizBlue = matrizRGB.get(2);
        for (int i = 0; i < 3; i++) {
            int[][] matriz = matrizRGB.get(i);
            int[][] novaMatriz = new int[altura][largura];
            for (int j = 0; j < altura; j++) {
                for (int k = 0; k < largura; k++) {
                    int corDominante = Math.max(matrizRed[j][k], Math.max(matrizGreen[j][k], matrizBlue[j][k]));
                    novaMatriz[j][k] = matriz[j][k] >= corDominante ? corDominante : 0;
                }
            }
            novaMatrizRGB.add(novaMatriz);
        }
        gerarImagem(novaMatrizRGB.get(0), novaMatrizRGB.get(1), novaMatrizRGB.get(2));
    }

    public void escalaCinzaEscuro(List<int[][]> matrizRGB) {
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;

        int[][] matrizRed = matrizRGB.get(0);
        int[][] matrizGreen = matrizRGB.get(1);
        int[][] matrizBlue = matrizRGB.get(2);
        int[][] matrizCinzaEscuro = new int[altura][largura];

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                int corMenorValor = Math.min(matrizRed[i][j], Math.min(matrizGreen[i][j], matrizBlue[i][j]));
                matrizCinzaEscuro[i][j] = corMenorValor;
            }
        }

        gerarImagem(matrizCinzaEscuro, matrizCinzaEscuro, matrizCinzaEscuro);
    }

    public void escalaCinzaClaro(List<int[][]> matrizRGB) {
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;
        int[][] matrizRed = matrizRGB.get(0);
        int[][] matrizGreen = matrizRGB.get(1);
        int[][] matrizBlue = matrizRGB.get(2);
        int[][] matrizCinzaClaro = new int[altura][largura];
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                int corMaiorValor = Math.max(matrizRed[i][j], Math.max(matrizGreen[i][j], matrizBlue[i][j]));
                matrizCinzaClaro[i][j] = corMaiorValor;
            }
        }
        gerarImagem(matrizCinzaClaro, matrizCinzaClaro, matrizCinzaClaro);
    }

    public void fazerUniao(List<int[][]> matrizRGB) {
        Object chaveEspera = new Object();
        ProvedorImagens provedorImagens = null;//ProvedorImagens.obterProvedor(chaveEspera, 2);

        new Thread(() -> {
            synchronized (chaveEspera) {
                try {
                    chaveEspera.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }

            BufferedImage imagem1 = provedorImagens.get(0);
            BufferedImage imagem2 = provedorImagens.get(1);

            int largura = imagem1.getWidth();
            int altura = imagemSelecionada.getHeight();

            List<int[][]> matrizRGB1 = obterEArmazenarMatrizRGB(imagem1);
            List<int[][]> matrizRGB2 = obterEArmazenarMatrizRGB(imagem2);

            int[][] binarizada1 = obterImagemBinaria(matrizRGB1);
            int[][] binarizada2 = obterImagemBinaria(matrizRGB2);

            int[][] uniao = binarizada1;

            for (int i = 0; i < altura; i++) {
                for (int j = 0; j < largura; j++) {
                    if (binarizada2[i][j] == PRETO) {
                        uniao[i][j] = PRETO;
                    }
                }
            }

            gerarImagem(uniao, uniao, uniao);

        }).start();
    }

    public void fazerIntersecao(List<int[][]> matrizRGB) {
        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        BufferedImage imagemSelecionadaAntiga = imagemSelecionada;

        imagemSelecionada = imagens.get(0);
        List<int[][]> matrizRGB1 = obterEArmazenarMatrizRGB();
        imagemSelecionada = imagens.get(1);
        List<int[][]> matrizRGB2 = obterEArmazenarMatrizRGB();

        BufferedImage imagemSelecionada = imagemSelecionadaAntiga;
        obterEArmazenarMatrizRGB();

        int[][] binarizada1 = obterImagemBinaria(matrizRGB1);
        int[][] binarizada2 = obterImagemBinaria(matrizRGB2);

        int[][] intersecao = binarizada1;

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                if (binarizada1[i][j] != binarizada2[i][j]) {
                    intersecao[i][j] = BRANCO;
                }
            }
        }

        gerarImagem("PintarImagem", intersecao, intersecao, intersecao);
    }

    public void escolhaDoUsuario(List<int[][]> matrizRGB) {
        int altura = matrizRGB.get(0).length;
        int largura = matrizRGB.get(0)[0].length;
        int[][] matrizRed = obterMatrizVermelha();
        int[][] matrizGreen = obterMatrizVerde();
        int[][] matrizBlue = obterMatrizAzul();
        String corEscolhida = JOptionPane.showInputDialog("Escolha uma cor: r, g ou b");
        if (!corEscolhida.matches("[rgb]")) {
            JOptionPane.showMessageDialog(null, "Cor inválida");
            return;
        }
        int matrizIndexSelecionado = corEscolhida.equals("r") ? 0 : corEscolhida.equals("g") ? 1 : 2;
        for (int linha = 0; linha < altura; linha++) {
            for (int coluna = 0; coluna < largura; coluna++) {
                if (isPixelCorIgualEscolhida(linha, coluna, matrizIndexSelecionado)) {
                    // System.out.println("foi, linha: " + linha + ", coluna: " + coluna);
                    continue;
                }
                // System.out.println("não foi, linha: " + linha + ", coluna: " + coluna);
                // Tornar pixel Cinza
                int media = (matrizRed[linha][coluna] + matrizGreen[linha][coluna] + matrizBlue[linha][coluna]) / 3;
                matrizRed[linha][coluna] = media;
                matrizGreen[linha][coluna] = media;
                matrizBlue[linha][coluna] = media;
            }
        }
        gerarImagem(matrizRed, matrizGreen, matrizBlue);
    }

    private boolean isPixelCorIgualEscolhida(int linha, int coluna, int matrizIndexSelecionado) {
        for (int matrizIndexAtual = 0; matrizIndexAtual < 3; matrizIndexAtual++) {
            int[][] matrizCor = matrizRGB.get(matrizIndexAtual);
            int corAtual = matrizCor[linha][coluna];
            boolean isMatrizDaCorSelecionada = (matrizIndexAtual == matrizIndexSelecionado);
            if (isCorInvalida(isMatrizDaCorSelecionada, corAtual)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCorInvalida(boolean isMatrizDaCorSelecionada, int corAtual) {
        return (!isMatrizDaCorSelecionada && corAtual >= 167) || (isMatrizDaCorSelecionada && corAtual < 167);
    }

    private void qualODispositivo(List<int[][]> matrizRGB) {
        int meio_largura = imagemSelecionada.getWidth() / 2;
        int meio_altura = imagemSelecionada.getHeight() / 2;

        Point[] coordenadas = new Point[4];

        coordenadas[0] = encontrarPonto(Direcoes.BAIXO, matrizRGB, meio_largura);
        coordenadas[1] = encontrarPonto(Direcoes.CIMA, matrizRGB, meio_largura);
        coordenadas[2] = encontrarPonto(Direcoes.DIREITA, matrizRGB, meio_altura);
        coordenadas[3] = encontrarPonto(Direcoes.ESQUERDA, matrizRGB, meio_altura);

        for (Point coordenada : coordenadas) {
            if (coordenada == null) {
                JOptionPane.showMessageDialog(null, "Não foi possível medir o dispositivo");
                return;
            }
        }

        int altura_dispositivo = coordenadas[1].y - coordenadas[0].y;
        int largura_dispositivo = coordenadas[3].x - coordenadas[2].x;

        if (largura_dispositivo > altura_dispositivo) {
            JOptionPane.showMessageDialog(null, "É um celular!");
        } else {
            JOptionPane.showMessageDialog(null, "É uma caneta!");
        }
    }

    private enum Direcoes {
        CIMA, BAIXO, ESQUERDA, DIREITA
    }

    private Point encontrarPonto(Direcoes direcao, List<int[][]> matrizRGB, int coord) {
        int matrizRed[][] = matrizRGB.get(0);
        int matrizGreen[][] = matrizRGB.get(1);
        int matrizBlue[][] = matrizRGB.get(2);

        int inicio = 0, passo = 0, limite = 0;
        int altura = matrizRed.length, largura = matrizRed[0].length;

        switch (direcao) {
            case BAIXO -> {
                inicio = 0;
                limite = altura - 1;
                passo = 1;
            }
            case CIMA -> {
                inicio = altura - 1;
                limite = 0;
                passo = -1;
            }
            case DIREITA -> {
                inicio = 0;
                limite = largura - 1;
                passo = 1;
            }
            case ESQUERDA -> {
                inicio = largura - 1;
                limite = 0;
                passo = -1;
            }
        }

        for (int i = inicio; i != limite + passo; i += passo) {
            int x = coord, y = i;

            if (direcao == Direcoes.ESQUERDA || direcao == Direcoes.DIREITA) {
                x = i;
                y = coord;
            }

            int r = matrizRed[y][x];
            int g = matrizGreen[y][x];
            int b = matrizBlue[y][x];
            int menorCor = Math.min(r, Math.min(g, b));

            int somaDiferenca = Math.abs(r - menorCor) + Math.abs(g - menorCor) + Math.abs(b - menorCor);

            if (somaDiferenca > DIFERENCA_TOTAL_MAXIMA) {
                return new Point(x, y);
            }
        }

        return null;
    }

    private void redimensionarImagem(List<int[][]> matrizRGB) {
        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        String resposta = JOptionPane.showInputDialog("Digite o fator de redimensionamento");
        double fator = 0;

        try {
            fator = Double.valueOf(resposta);
            if (fator <= 0) {
                throw new IllegalArgumentException("O fator de redimensionamento deve ser maior que zero.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Valor inválido digitado!");
            return;
        }

        fator /= 100;

        int novaLargura = (int) (largura * fator);
        int novaAltura = (int) (altura * fator);

        double proporcao = 1 / fator;

        int[][] matrizRed = obterMatrizVermelha();
        int[][] matrizGreen = obterMatrizVerde();
        int[][] matrizBlue = obterMatrizAzul();

        int[][] novaMatrizRed = new int[novaAltura][novaLargura];
        int[][] novaMatrizGreen = new int[novaAltura][novaLargura];
        int[][] novaMatrizBlue = new int[novaAltura][novaLargura];

        for (int i = 0; i < novaAltura; i++) {
            for (int j = 0; j < novaLargura; j++) {
                novaMatrizRed[i][j] = matrizRed[(int) (i * proporcao)][(int) (j * proporcao)];
                novaMatrizGreen[i][j] = matrizGreen[(int) (i * proporcao)][(int) (j * proporcao)];
                novaMatrizBlue[i][j] = matrizBlue[(int) (i * proporcao)][(int) (j * proporcao)];
            }
        }

        gerarImagem(novaMatrizRed, novaMatrizGreen, novaMatrizBlue);
    }

    private void rotacionarImagem(List<int[][]> matrizRGB) {
        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();
        List<int[][]> novaMatrizRGB = new LinkedList<>();
        for (int i = 0; i < 3; i++) {
            int[][] novaMatrizCor = new int[largura][altura];
            int[][] matrizAtual = matrizRGB.get(i);
            for (int j = 0; j < altura; j++) {
                for (int k = 0; k < largura; k++) {
                    novaMatrizCor[k][j] = matrizAtual[j][k];
                }
            }
            novaMatrizRGB.add(novaMatrizCor);
        }
        gerarImagem(novaMatrizRGB.get(0), novaMatrizRGB.get(1), novaMatrizRGB.get(2));
    }

    private void rotacionarPersonalizado(List<int[][]> matrizRGB) {

        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        int alturaAdicional = 0;

        double angulo = 0;

        int maiorX = 0, menorY = 0;

        try {
            angulo = Double.valueOf(JOptionPane.showInputDialog("Digite o ângulo em graus"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Digite um valor válido", "Erro!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        angulo = Math.toRadians(angulo);

        angulo = angulo % 360;
        if (angulo < 0) {
            angulo = angulo + 360;
        }

        if (angulo < 90) {
            maiorX = obterX(0, altura, angulo);
            menorY = obterY(0, altura, angulo);
        }/*
        else if(angulo < 180) {
            maiorX = obterX(largura, altura, angulo);
            menorY = obterY(largura, altura, angulo);
        }
        else if(angulo < 270) {
            maiorX = obterX(largura, 0, angulo);
            menorY = obterY(largura, 0, angulo);
        }
        else {
            maiorX = obterX(0, 0, angulo);
            menorY = obterY(0, 0, angulo);
        }*/

        int diferencaX = largura - maiorX;
        int diferencaY = -menorY;

        int novaLargura = largura + (2 * diferencaX);
        int novaAltura = altura + (2 * diferencaY);

        System.out.println("maiorX: " + maiorX + ", menorY: " + menorY);
        System.out.println("diferencaX: " + diferencaX + ", menorY: " + diferencaY);

        int[][] matrizRed = new int[novaAltura][novaLargura];
        int[][] matrizBlue = new int[novaAltura][novaLargura];
        int[][] matrizGreen = new int[novaAltura][novaLargura];

        int novoX = 0;
        int novoY = 0;

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                novoX = diferencaX + obterX(j, i, angulo);
                novoY = diferencaY + obterY(j, i, angulo);

                matrizRed[novoY][novoX] = matrizRGB.get(0)[i][j];
                matrizGreen[novoY][novoX] = matrizRGB.get(1)[i][j];
                matrizBlue[novoY][novoX] = matrizRGB.get(2)[i][j];
            }
        }

        gerarImagem(matrizRed, matrizGreen, matrizBlue);
    }

    private int obterX(int x, int y, double angulo) {
        return (int) (x * Math.cos(angulo) - y * Math.sin(angulo));
    }

    private int obterY(int x, int y, double angulo) {
        return (int) (x * Math.sin(angulo) - y * Math.cos(angulo));
    }

    private void FiltroMedia(List<int[][]> matrizRGB) {
        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        // Solicita ao usuário o tamanho da matriz
        String[] options = {"3x3", "5x5"};
        int resposta = JOptionPane.showOptionDialog(null, "Escolha o tamanho da matriz para o filtro de média:",
                "Tamanho da Matriz", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        int tamanhoMatriz;
        if (resposta == 0) {
            tamanhoMatriz = 3;
        } else {
            tamanhoMatriz = 5;
        }

        int[][] resultadoRed = new int[altura][largura];
        int[][] resultadoGreen = new int[altura][largura];
        int[][] resultadoBlue = new int[altura][largura];

        int[][] matrizRed = obterMatrizVermelha();
        int[][] matrizGreen = obterMatrizVerde();
        int[][] matrizBlue = obterMatrizAzul();

        // Percorre cada pixel da imagem
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                int somaRed = 0;
                int somaGreen = 0;
                int somaBlue = 0;
                int count = 0;

                // Percorre os pixels vizinhos em uma janela tamanhoMatriz x tamanhoMatriz
                for (int i = -tamanhoMatriz / 2; i <= tamanhoMatriz / 2; i++) {
                    for (int j = -tamanhoMatriz / 2; j <= tamanhoMatriz / 2; j++) {
                        int pixelX = x + i;
                        int pixelY = y + j;

                        // Verifica se o pixel vizinho está dentro dos limites da imagem
                        if (pixelX >= 0 && pixelX < largura && pixelY >= 0 && pixelY < altura) {
                            somaRed += matrizRed[pixelY][pixelX];
                            somaGreen += matrizGreen[pixelY][pixelX];
                            somaBlue += matrizBlue[pixelY][pixelX];
                            count++;
                        }
                    }
                }

                // Calcula a média dos valores RGB apenas se houver vizinhos válidos
                if (count > 0) {
                    resultadoRed[y][x] = somaRed / count;
                    resultadoGreen[y][x] = somaGreen / count;
                    resultadoBlue[y][x] = somaBlue / count;
                } else {
                    // Se nenhum vizinho válido for encontrado, mantém o valor original
                    resultadoRed[y][x] = matrizRed[y][x];
                    resultadoGreen[y][x] = matrizGreen[y][x];
                    resultadoBlue[y][x] = matrizBlue[y][x];
                }
            }
        }

        // Gerar a imagem processada com os novos valores RGB
        gerarImagem(resultadoRed, resultadoGreen, resultadoBlue);
    }

    private void FiltroMediana(List<int[][]> matrizRGB) {

        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        // Solicita ao usuário o tamanho da matriz
        String[] options = {"3x3", "5x5", "7x7"};
        int resposta = JOptionPane.showOptionDialog(null, "Escolha o tamanho da matriz para o filtro de mediana:",
                "Tamanho da Matriz", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        int tamanhoMatriz;

        if (resposta == 0) {
            tamanhoMatriz = 3;

        } else if (resposta == 5) {
            tamanhoMatriz = 5;
        } else {
            tamanhoMatriz = 7;
        }

        // Simulação das matrizes de canal de cores
        int[][] matrizRed = obterMatrizVermelha();
        int[][] matrizGreen = obterMatrizVerde();
        int[][] matrizBlue = obterMatrizAzul();

        int[][] resultadoRed = new int[altura][largura];
        int[][] resultadoGreen = new int[altura][largura];
        int[][] resultadoBlue = new int[altura][largura];

        // Percorre cada pixel da imagem
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                List<Integer> vizinhosRed = new ArrayList<>();
                List<Integer> vizinhosGreen = new ArrayList<>();
                List<Integer> vizinhosBlue = new ArrayList<>();
                int index = 0;

                // Percorre os pixels vizinhos em uma janela tamanhoMatriz x tamanhoMatriz
                for (int i = -tamanhoMatriz / 2; i <= tamanhoMatriz / 2; i++) {
                    for (int j = -tamanhoMatriz / 2; j <= tamanhoMatriz / 2; j++) {
                        int pixelX = x + i;
                        int pixelY = y + j;

                        // Verifica se o pixel vizinho está dentro dos limites da imagem
                        if (pixelX >= 0 && pixelX < largura && pixelY >= 0 && pixelY < altura) {
                            vizinhosRed.add(matrizRed[pixelY][pixelX]);
                            vizinhosGreen.add(matrizGreen[pixelY][pixelX]);
                            vizinhosBlue.add(matrizBlue[pixelY][pixelX]);
                            index++;
                        }
                    }
                }

                // Ordena os arrays de vizinhos
                Collections.sort(vizinhosRed);
                Collections.sort(vizinhosGreen);
                Collections.sort(vizinhosBlue);

                // Calcula a mediana dos valores RGB
                int medianaRed = calcularMediana(vizinhosRed);
                int medianaGreen = calcularMediana(vizinhosGreen);
                int medianaBlue = calcularMediana(vizinhosBlue);

                // Atualiza os resultados com os valores da mediana
                resultadoRed[y][x] = medianaRed;
                resultadoGreen[y][x] = medianaGreen;
                resultadoBlue[y][x] = medianaBlue;

            }
        }

        // Gerar a imagem processada com os novos valores RGB
        gerarImagem(resultadoRed, resultadoGreen, resultadoBlue);

    }
    

    private void filtroSobel(List<int[][]> matrizRGB) {
    	int largura = imagemSelecionada.getWidth();
    	int altura = imagemSelecionada.getHeight();

    	int[][] matrizRed = obterMatrizVermelha();
    	int[][] matrizGreen = obterMatrizVerde();
    	int[][] matrizBlue = obterMatrizAzul();

    	int[][] resultadoRed = new int[altura][largura];
    	int[][] resultadoGreen = new int[altura][largura];
    	int[][] resultadoBlue = new int[altura][largura];

    	int[][] sobelX = {
    			{-1, 0, 1},
    			{-2, 0, 2},
    			{-1, 0, 1}
    	};

    	int[][] sobelY = {
    			{-1, -2, -1},
    			{ 0,  0,  0},
    			{ 1,  2,  1}
    	};

    	for (int x = 1; x < largura - 1; x++) {
    		for (int y = 1; y < altura - 1; y++) {
    			int gxRed = 0, gyRed = 0;
    			int gxGreen = 0, gyGreen = 0;
    			int gxBlue = 0, gyBlue = 0;

    			for (int i = -1; i <= 1; i++) {
    				for (int j = -1; j <= 1; j++) {
    					int pixelX = x + i;
    					int pixelY = y + j;

    					gxRed += matrizRed[pixelY][pixelX] * sobelX[i + 1][j + 1];
    					gyRed += matrizRed[pixelY][pixelX] * sobelY[i + 1][j + 1];

    					gxGreen += matrizGreen[pixelY][pixelX] * sobelX[i + 1][j + 1];
    					gyGreen += matrizGreen[pixelY][pixelX] * sobelY[i + 1][j + 1];

    					gxBlue += matrizBlue[pixelY][pixelX] * sobelX[i + 1][j + 1];
    					gyBlue += matrizBlue[pixelY][pixelX] * sobelY[i + 1][j + 1];
    				}
    			}

    			int magRed = Math.min(255, (int) Math.sqrt(gxRed * gxRed + gyRed * gyRed));
    			int magGreen = Math.min(255, (int) Math.sqrt(gxGreen * gxGreen + gyGreen * gyGreen));
    			int magBlue = Math.min(255, (int) Math.sqrt(gxBlue * gxBlue + gyBlue * gyBlue));

    			resultadoRed[y][x] = magRed;
    			resultadoGreen[y][x] = magGreen;
    			resultadoBlue[y][x] = magBlue;
    		}
    	}

    	gerarImagem(resultadoRed, resultadoGreen, resultadoBlue);
    }

    
    
    private int calcularMediana(List<Integer> valores) {
        int tamanho = valores.size();
        if (tamanho == 0) {
            return 0;
        }

        if (tamanho % 2 == 0) {
            int meio = tamanho / 2;
            return (valores.get(meio - 1) + valores.get(meio)) / 2;
        } else {
            return valores.get(tamanho / 2);
        }
    }

    private void FiltroGaussiano(List<int[][]> matrizRGB) {

        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        // Solicita ao usuário o tamanho da matriz
        String[] options = {"3x3", "5x5", "7x7"};
        int resposta = JOptionPane.showOptionDialog(null, "Escolha o tamanho da matriz para o filtro de média:",
                "Tamanho da Matriz", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        int tamanhoMatriz;
        if (resposta == 0) {
            tamanhoMatriz = 3;
        } else if (resposta == 1) {
            tamanhoMatriz = 5;
        } else {
            tamanhoMatriz = 7;
        }

        int[][] resultadoRed = new int[altura][largura];
        int[][] resultadoGreen = new int[altura][largura];
        int[][] resultadoBlue = new int[altura][largura];

        int[][] matrizRed = obterMatrizVermelha();
        int[][] matrizGreen = obterMatrizVerde();
        int[][] matrizBlue = obterMatrizAzul();

        int[][] matrizGaussiana = null;

        matrizGaussiana = obterMatrizGaussiana(tamanhoMatriz);

        // Percorre cada pixel da imagem
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                int somaRed = 0;
                int somaGreen = 0;
                int somaBlue = 0;
                int total = 0;

                // Percorre os pixels vizinhos em uma janela tamanhoMatriz x tamanhoMatriz
                for (int offSetX = -tamanhoMatriz / 2; offSetX <= tamanhoMatriz / 2; offSetX++) {
                    for (int offSetY = -tamanhoMatriz / 2; offSetY <= tamanhoMatriz / 2; offSetY++) {
                        int pixelX = x + offSetX;
                        int pixelY = y + offSetY;

                        // Verifica se o pixel vizinho está dentro dos limites da imagem
                        if (pixelX >= 0 && pixelX < largura && pixelY >= 0 && pixelY < altura) {
                            int pesoMatrizGaussiano = matrizGaussiana[offSetY + tamanhoMatriz / 2][offSetX + tamanhoMatriz / 2];
                            somaRed += matrizRed[pixelY][pixelX] * pesoMatrizGaussiano;
                            somaGreen += matrizGreen[pixelY][pixelX] * pesoMatrizGaussiano;
                            somaBlue += matrizBlue[pixelY][pixelX] * pesoMatrizGaussiano;
                            total += pesoMatrizGaussiano;
                        }
                    }
                }

                // Calcula a média dos valores RGB apenas se houver vizinhos válidos
                if (total > 0) {
                    resultadoRed[y][x] = somaRed / total;
                    resultadoGreen[y][x] = somaGreen / total;
                    resultadoBlue[y][x] = somaBlue / total;
                } else {
                    // Se nenhum vizinho válido for encontrado, mantém o valor original
                    resultadoRed[y][x] = matrizRed[y][x];
                    resultadoGreen[y][x] = matrizGreen[y][x];
                    resultadoBlue[y][x] = matrizBlue[y][x];
                }
            }
        }

        // Gerar a imagem processada com os novos valores RGB
        gerarImagem(resultadoRed, resultadoGreen, resultadoBlue);

    }

    private int[][] obterMatrizGaussiana(int tamanhoMatriz) {
        int[][] matrizGaussiana = null;
        if (tamanhoMatriz == 3) {
            matrizGaussiana = new int[][]{
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}

            };
        } else if (tamanhoMatriz == 5) {
            matrizGaussiana = new int[][]{
                {1, 4, 7, 4, 1},
                {4, 16, 26, 16, 4},
                {7, 26, 41, 26, 7},
                {4, 16, 26, 16, 4},
                {1, 4, 7, 4, 1}
            };

        } else {
            matrizGaussiana = new int[][]{
                {0, 0, 1, 2, 1, 0, 0},
                {0, 3, 13, 22, 13, 3, 0},
                {1, 13, 59, 97, 59, 13, 1},
                {2, 22, 97, 159, 97, 22, 2},
                {1, 13, 59, 97, 59, 13, 1},
                {0, 3, 13, 22, 13, 3, 0},
                {0, 0, 1, 2, 1, 0, 0}
            };
        }

        return matrizGaussiana;
    }

    private void converterImagem(List<int[][]> matrizRGB) {

        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        int[][] matrizRed = obterMatrizVermelha();
        int[][] matrizGreen = obterMatrizVerde();
        int[][] matrizBlue = obterMatrizAzul();

        float[][] hMatriz = new float[altura][largura];
        float[][] sMatriz = new float[altura][largura];
        float[][] vMatriz = new float[altura][largura];

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                float r = matrizRed[i][j] / 255.0f;
                float g = matrizGreen[i][j] / 255.0f;
                float b = matrizBlue[i][j] / 255.0f;

                float max = Math.max(r, Math.max(g, b));
                float min = Math.min(r, Math.min(g, b));

                float v = max;
                float s = (max == 0) ? 0 : (max - min) / max;
                float h = 0;

                if (max != min) {
                    if (max == r) {
                        h = (g - b) / (max - min);
                    } else if (max == g) {
                        h = 2 + (b - r) / (max - min);
                    } else {
                        h = 4 + (r - g) / (max - min);
                    }

                    h *= 60;
                    if (h < 0) {
                        h += 360;
                    }
                }

                hMatriz[i][j] = h;
                sMatriz[i][j] = s;
                vMatriz[i][j] = v;
            }
        }

        float[][] novoH = aumentarValorParaMaximo(hMatriz, 'h');
        float[][] novoS = aumentarValorParaMaximo(sMatriz, 's');
        float[][] novoV = aumentarValorParaMaximo(vMatriz, 'v');

        List<int[][]> novaMatrizRGB = converterParaRGB(novoH, sMatriz, vMatriz);
        gerarImagem("H máximo", novaMatrizRGB.get(0), novaMatrizRGB.get(1), novaMatrizRGB.get(2));

        novaMatrizRGB = converterParaRGB(hMatriz, novoS, vMatriz);
        gerarImagem("S máximo", novaMatrizRGB.get(0), novaMatrizRGB.get(1), novaMatrizRGB.get(2));

        novaMatrizRGB = converterParaRGB(hMatriz, sMatriz, novoV);
        gerarImagem("V máximo", novaMatrizRGB.get(0), novaMatrizRGB.get(1), novaMatrizRGB.get(2));
    }

    private float[][] aumentarValorParaMaximo(float[][] matriz, char tipo) {
        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        float[][] novaMatriz = new float[altura][largura];

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                novaMatriz[i][j] = tipo == 'h' ? 360 : 1;
            }
        }

        return novaMatriz;
    }

    private List<int[][]> converterParaRGB(float[][] hMatriz, float[][] sMatriz, float[][] vMatriz) {
        int largura = imagemSelecionada.getWidth();
        int altura = imagemSelecionada.getHeight();

        int[][] rMatriz = new int[altura][largura];
        int[][] gMatriz = new int[altura][largura];
        int[][] bMatriz = new int[altura][largura];

        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                float h = hMatriz[i][j];
                float s = sMatriz[i][j];
                float v = vMatriz[i][j];

                float c = v * s;
                float x = c * (1 - Math.abs((h / 60) % 2 - 1));
                float m = v - c;

                float r = 0, g = 0, b = 0;

                if (h >= 0 && h < 60) {
                    r = c;
                    g = x;
                } else if (h < 120) {
                    r = x;
                    g = c;
                } else if (h < 180) {
                    g = c;
                    b = x;
                } else if (h < 240) {
                    g = x;
                    b = c;
                } else if (h < 300) {
                    r = x;
                    b = c;
                } else if (h < 360) {
                    r = c;
                    b = x;
                }

                rMatriz[i][j] = Math.round((r + m) * 255);
                gMatriz[i][j] = Math.round((g + m) * 255);
                bMatriz[i][j] = Math.round((b + m) * 255);
            }
        }

        return new LinkedList<>(List.of(rMatriz, gMatriz, bMatriz));
    }

    public ImageIFMG() {
        super("PhotoIFMG");
        setSize(LARGURA_JANELA, ALTURA_JANELA);
        instanciarComponentes();
        adicionarComponentes();
        criarListeners();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void instanciarComponentes() {
        jDesktopPaneImagem = new JDesktopPane();
        jMenuBar = new JMenuBar();
        jMenuAbrir = new JMenu("Abrir");
        jMenuProcessar = new JMenu("Processar");
        jMenuSalvar = new JMenu("Salvar");
        jMenuItemAbrirImagem = new JMenuItem("Abrir uma imagem de arquivo");
        jMenuItemCriarInternalFrame = new JMenuItem("Internal Frame");
        jMenuItemsProcessar = new JMenuItem[18];
        jMenuItemSalvar = new JMenuItem("Salvar Imagem");
    }

    private void adicionarComponentes() {
        getContentPane().add(jDesktopPaneImagem);
        jMenuAbrir.add(jMenuItemAbrirImagem);
        jMenuAbrir.add(jMenuItemCriarInternalFrame);
        jMenuSalvar.add(jMenuItemSalvar);
        jMenuBar.add(jMenuAbrir);
        jMenuBar.add(jMenuProcessar);
        jMenuBar.add(jMenuSalvar);
        setJMenuBar(jMenuBar);
        String[] menuItemTexts = {"Escala de cinza", "Imagem binária", "Negativa", "Cor dominante", "Cinza escuro",
            "Cinza claro", "Escolha do usuário", "Qual o dispositivo", "Redimensionar", "Rotacionar",
            "Converter Formato", "União", "Interseção", "Rotacionar Personalizado", "Filtro Média", "Filtro Gaussiano", "Filtro da Mediana", "Filtro Sobel"};
        for (int i = 0; i < menuItemTexts.length; i++) {
            jMenuItemsProcessar[i] = new JMenuItem(menuItemTexts[i]);
            jMenuProcessar.add(jMenuItemsProcessar[i]);
        }
    }

    private void criarListeners() {
        jMenuItemsProcessar[0].addActionListener(e -> escalaCinza(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[1].addActionListener(e -> imagemBinaria(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[2].addActionListener(e -> imagemNegativa(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[3].addActionListener(e -> corDominante(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[4].addActionListener(e -> escalaCinzaEscuro(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[5].addActionListener(e -> escalaCinzaClaro(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[6].addActionListener(e -> escolhaDoUsuario(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[7].addActionListener(e -> qualODispositivo(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[8].addActionListener(e -> redimensionarImagem(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[9].addActionListener(e -> rotacionarImagem(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[10].addActionListener(e -> converterImagem(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[11].addActionListener(e -> fazerUniao(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[12].addActionListener(e -> fazerIntersecao(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[13].addActionListener(e -> rotacionarPersonalizado(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[14].addActionListener(e -> FiltroMedia(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[15].addActionListener(e -> FiltroGaussiano(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[16].addActionListener(e -> FiltroMediana(obterEArmazenarMatrizRGB()));
        jMenuItemsProcessar[17].addActionListener(e -> filtroSobel(obterEArmazenarMatrizRGB()));

        jMenuItemSalvar.addActionListener(e -> salvarImagem());
        jMenuItemCriarInternalFrame.addActionListener((e) -> {
            JInternalFrame frame = new JInternalFrame("Exemplo", true, true, true, true);
            JPanelImagem panel = new JPanelImagem(imagemSelecionada);
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.pack();
            jDesktopPaneImagem.add(frame);
            frame.setVisible(true);
        });
        jMenuItemAbrirImagem.addActionListener((e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(
                    new javax.swing.filechooser.FileNameExtensionFilter("Imagens", "jpg", "png", "jpeg", "tiff"));
            int resultado = fileChooser.showOpenDialog(null);
            if (resultado == JFileChooser.CANCEL_OPTION) {
                return;
            }
            String caminho = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                imagemSelecionada = ImageIO.read(new File(caminho));
                imagens.add(imagemSelecionada);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            criarJanelaDaImagem(imagemSelecionada, "Exemplo");
        });
    }

    public void salvarImagem() {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagens PNG", "png"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int resultado = fileChooser.showSaveDialog(null);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File arquivoSelecionado = fileChooser.getSelectedFile();
            if (!arquivoSelecionado.getName().toLowerCase().endsWith(".png")) {
                arquivoSelecionado = new File(arquivoSelecionado.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(imagemSelecionada, "png", arquivoSelecionado);
            } catch (IOException e) {
                //Tratar erro
            }
        }
    }

    public List<int[][]> obterEArmazenarMatrizRGB() {
        return obterEArmazenarMatrizRGB(imagemSelecionada);
    }

    // ler matrizes da imagem
    public List<int[][]> obterEArmazenarMatrizRGB(BufferedImage imagem) {
        int altura = imagem.getHeight();
        int largura = imagem.getWidth();

        int[][] redMatriz = new int[altura][largura];
        int[][] greenMatriz = new int[altura][largura];
        int[][] blueMatriz = new int[altura][largura];

        for (int imagemLinha = 0; imagemLinha < altura; imagemLinha++) {
            for (int imagemColuna = 0; imagemColuna < largura; imagemColuna++) {
                redMatriz[imagemLinha][imagemColuna] = obterPixelData(imagem, imagemColuna, imagemLinha)[0];
                greenMatriz[imagemLinha][imagemColuna] = obterPixelData(imagem, imagemColuna, imagemLinha)[1];
                blueMatriz[imagemLinha][imagemColuna] = obterPixelData(imagem, imagemColuna, imagemLinha)[2];
            }
        }

        List<int[][]> pixeisRGB = new LinkedList<>(List.of(redMatriz, greenMatriz, blueMatriz));
        if (imagemSelecionada == imagem) {
            matrizRGB = pixeisRGB;
        }
        return pixeisRGB;
    }

    // cria imagem da matriz
    private void gerarImagem(int matrizRed[][], int matrizGreen[][], int matrizBlue[][]) {
        gerarImagem("Processada", matrizRed, matrizGreen, matrizBlue);
    }

    private void gerarImagem(String titulo, int matrizRed[][], int matrizGreen[][], int matrizBlue[][]) {
        int[] pixels = new int[matrizRed.length * matrizRed[0].length * 3];
        BufferedImage novaImagem = new BufferedImage(matrizRed[0].length, matrizRed.length, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = novaImagem.getRaster();
        int posicao = 0;
        for (int imagemLinha = 0; imagemLinha < matrizRed.length; imagemLinha++) {
            for (int imagemColuna = 0; imagemColuna < matrizRed[0].length; imagemColuna++) {
                pixels[posicao] = matrizRed[imagemLinha][imagemColuna];
                pixels[posicao + 1] = matrizGreen[imagemLinha][imagemColuna];
                pixels[posicao + 2] = matrizBlue[imagemLinha][imagemColuna];
                posicao += 3;
            }
        }
        raster.setPixels(0, 0, matrizRed[0].length, matrizRed.length, pixels);
        imagens.add(novaImagem);
        criarJanelaDaImagem(novaImagem, titulo);
    }

    private void criarJanelaDaImagem(BufferedImage novaImagem, String titulo) {
        int antigaLarguraImagem = novaImagem.getWidth();
        int antigaAlturaImagem = novaImagem.getHeight();
        int novaLarguraFrame;
        int novaAlturaFrame;
//        if(antigaLarguraImagem > antigaAlturaImagem) {
//            novaLarguraFrame = LARGURA_JANELA * 8 / 10;
//            novaAlturaFrame = antigaAlturaImagem * novaLarguraFrame / antigaLarguraImagem;
//        }
//        else {
//            novaAlturaFrame = ALTURA_JANELA * 8 / 10;
//            novaLarguraFrame = antigaLarguraImagem * novaAlturaFrame / antigaAlturaImagem;
//        }
//      
//        Image imagemRedimensionada = novaImagem.getScaledInstance(novaLarguraFrame, novaAlturaFrame, java.awt.Image.SCALE_SMOOTH);
        JInternalFrame frame = new JInternalFrame(titulo, true, true, true, true);
        JPanelImagem panel = new JPanelImagem(novaImagem);
//        JPanelImagem panel = new JPanelImagem(new ImageIcon(imagemRedimensionada));
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        jDesktopPaneImagem.add(frame);

        frame.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                frameAtual = frame;
                imagemSelecionada = obterImagemDoFrameAtual();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });

        panel.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.equals(KeyEvent.VK_ENTER)) {
                    //provedorImagensGlobal.atualizarProvedores(panel.getImage());
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        frame.setVisible(true);
        frame.requestFocus();

        if (titulo.equals("PintarImagem")) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    panel.setImageIcon(imagens.get(0));
                    panel.repaint();

                } catch (InterruptedException ex) {
                    Logger.getLogger(ImageIFMG.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }).start();
        }
    }

    public BufferedImage obterImagemDoFrameAtual() {
        if (frameAtual != null) {
            Component[] components = frameAtual.getContentPane().getComponents();
            if (components.length == 1 && components[0] instanceof JPanelImagem) {
                JPanelImagem panelImagem = (JPanelImagem) components[0];
                return panelImagem.getImage();
            }
        }
        return null;
    }

    public int[][] obterMatrizVermelha() {
        return matrizRGB.get(0);
    }

    public int[][] obterMatrizVerde() {
        return matrizRGB.get(1);
    }

    public int[][] obterMatrizAzul() {
        return matrizRGB.get(2);
    }

    private static int[] obterPixelData(BufferedImage imagem, int x, int y) {
        int argb = imagem.getRGB(x, y);
        int rgb[] = new int[]{(argb >> 16) & 0xff, // red
            (argb >> 8) & 0xff, // green
            (argb) & 0xff // blue
    };
        return rgb;

    }

    class JPanelImagem extends JPanel {

        private ImageIcon imageIcon;
        private BufferedImage bufferedImage;

        public JPanelImagem(BufferedImage bufferedImage) {
            this.bufferedImage = bufferedImage;
            this.imageIcon = new ImageIcon(bufferedImage);
        }

        public void setImageIcon(BufferedImage bufferedImage) {
            this.bufferedImage = bufferedImage;
            this.imageIcon = new ImageIcon(bufferedImage);
        }

        public BufferedImage getImage() {
            return bufferedImage;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponents(g);
            imageIcon.paintIcon(this, g, 0, 0);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(imageIcon.getIconWidth(), imageIcon.getIconHeight());
        }
    }

    public static void main(String[] args) {
        ImageIFMG app = new ImageIFMG();
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
