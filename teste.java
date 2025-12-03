package logicasjava;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class teste extends JFrame {

    static String servidor = "//TBS0676791W11-1\\SQLEXPRESS:1433";
    static String bancoDedados = "CRM";
    private static final String CONNECTION_URL = "jdbc:sqlserver:" + servidor
            + ";databaseName=" + bancoDedados + ";integratedSecurity=true;trustServerCertificate=true;encrypt=true;";

    public static class Conexao {

        private Connection conectar() throws SQLException {
            return DriverManager.getConnection(CONNECTION_URL);
        }

        public boolean clienteExiste(String nome, String email) throws SQLException {
            String sql = "SELECT COUNT(*) AS Total FROM tb_clientes WHERE nome = ? AND email = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nome);
                ps.setString(2, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("Total") > 0;
                    }
                }
            }
            return false;
        }

        public void inserirCliente(String nome, String email, String telefone) throws SQLException {
            if (clienteExiste(nome, email)) {
                throw new SQLException("Cliente já cadastrado com mesmo nome e email");
            }
            String sql = "{CALL InserirClientes(?,?,?)}";
            try (Connection c = conectar(); CallableStatement stmt = c.prepareCall(sql)) {
                stmt.setString(1, nome);
                stmt.setString(2, email);
                stmt.setString(3, telefone);
                stmt.execute();
            }
        }

        public void atualizarNomeCliente(String nomeAtual, String novoNome) throws SQLException {
            // 1. Verificação de Duplicidade:
            String sqlVerifica = "SELECT COUNT(*) FROM tb_clientes WHERE nome = ?";
            try (Connection c = conectar(); PreparedStatement psVerifica = c.prepareStatement(sqlVerifica)) {
                psVerifica.setString(1, novoNome);
                try (ResultSet rs = psVerifica.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        if (!nomeAtual.equals(novoNome)) {
                            throw new SQLException("Já existe outro cliente com o nome '" + novoNome + "'. A atualização não foi realizada.");
                        }
                    }
                }
            }

            String sqlAtualiza = "UPDATE tb_clientes SET nome = ? WHERE nome = ?";
            try (Connection c = conectar(); PreparedStatement psAtualiza = c.prepareStatement(sqlAtualiza)) {
                psAtualiza.setString(1, novoNome);
                psAtualiza.setString(2, nomeAtual);

                int linhasAfetadas = psAtualiza.executeUpdate();

                if (linhasAfetadas == 0) {
                    throw new SQLException("Cliente não encontrado ou nome inalterado.");
                }
            }
        }

        public DefaultTableModel listarClientes() throws SQLException {
            String sql = "{CALL ListarClientes}";
            DefaultTableModel modelo = new DefaultTableModel(new String[]{"Nome", "Email", "Telefone"}, 0);
            try (Connection c = conectar(); CallableStatement stmt = c.prepareCall(sql); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("telefone")
                    });
                }
            }
            return modelo;
        }

        public void inserirContato(String nomeCliente, String descricao) throws SQLException {
            String sqlId = "SELECT id_cliente FROM tb_clientes WHERE nome = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sqlId)) {
                ps.setString(1, nomeCliente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idCliente = rs.getInt("id_cliente");
                        String sqlInsert = "{CALL InserirContatos(?,?)}";
                        try (CallableStatement stmt = c.prepareCall(sqlInsert)) {
                            stmt.setInt(1, idCliente);
                            stmt.setString(2, descricao);
                            stmt.execute();
                        }
                    } else {
                        throw new SQLException("Cliente não encontrado para inserir contato.");
                    }
                }
            }
        }

        public DefaultTableModel listarContatos() throws SQLException {
            String sql = "SELECT c.nome AS Cliente, ct.descricao AS Descricao "
                    + "FROM tb_contatos ct INNER JOIN tb_clientes c ON ct.id_cliente = c.id_cliente";
            DefaultTableModel modelo = new DefaultTableModel(new String[]{"Cliente", "Descrição"}, 0);
            try (Connection c = conectar(); PreparedStatement stmt = c.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getString("Cliente"),
                        rs.getString("Descricao")
                    });
                }
            }
            return modelo;
        }

        public void inserirRegistro(String nomeCliente, double valor, String titulo, String status) throws SQLException {
            String sqlId = "SELECT id_cliente FROM tb_clientes WHERE nome = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sqlId)) {
                ps.setString(1, nomeCliente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idCliente = rs.getInt("id_cliente");
                        String sqlInsert = "{CALL InserirRegistros(?,?,?,?)}";
                        try (CallableStatement stmt = c.prepareCall(sqlInsert)) {
                            stmt.setDouble(1, valor);
                            stmt.setString(2, titulo);
                            stmt.setString(3, status);
                            stmt.setInt(4, idCliente);
                            stmt.execute();
                        }
                    } else {
                        throw new SQLException("Cliente não encontrado para inserir registro.");
                    }
                }
            }
        }

        public DefaultTableModel listarRegistros() throws SQLException {
            String sql = "SELECT r.titulo AS Titulo, r.valor AS Valor, r.status AS Status, c.nome AS Cliente "
                    + "FROM tb_registros r INNER JOIN tb_clientes c ON r.id_cliente = c.id_cliente";
            DefaultTableModel modelo = new DefaultTableModel(new String[]{"Título", "Valor", "Status", "Cliente"}, 0);
            try (Connection c = conectar(); PreparedStatement stmt = c.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getString("Titulo"),
                        rs.getDouble("Valor"),
                        rs.getString("Status"),
                        rs.getString("Cliente")
                    });
                }
            }
            return modelo;
        }

        public void inserirProduto(String nome, String sku, String tipo, String modelo,
                double preco, String unidade, Double custo, String categoria,
                String status, String data) throws SQLException {
            String sql = "{CALL InserirProdutos(?,?,?,?,?,?,?,?,?,?)}";
            try (Connection c = conectar(); CallableStatement stmt = c.prepareCall(sql)) {
                stmt.setString(1, nome);
                stmt.setString(2, sku);
                stmt.setString(3, tipo);
                stmt.setString(4, modelo);
                stmt.setDouble(5, preco);
                stmt.setString(6, unidade);
                if (custo == null) {
                    stmt.setNull(7, Types.DECIMAL);
                } else {
                    stmt.setDouble(7, custo);
                }
                stmt.setString(8, categoria);
                stmt.setString(9, status);
                if (data == null || data.isEmpty()) {
                    stmt.setNull(10, Types.DATE);
                } else {
                    stmt.setDate(10, Date.valueOf(data));
                }
                stmt.execute();
            }
        }

        public void excluirCliente(String nome) throws SQLException {
            String sql = "DELETE FROM tb_clientes WHERE nome = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nome);
                ps.executeUpdate();
            }
        }

        public void excluirContato(String nomeCliente) throws SQLException {
            String sql = "DELETE ct FROM tb_contatos ct INNER JOIN tb_clientes c ON ct.id_cliente = c.id_cliente WHERE c.nome = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nomeCliente);
                ps.executeUpdate();
            }
        }

        public void excluirRegistro(String nomeCliente) throws SQLException {
            String sql = "DELETE r FROM tb_registros r INNER JOIN tb_clientes c ON r.id_cliente = c.id_cliente WHERE c.nome = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nomeCliente);
                ps.executeUpdate();
            }
        }

        public void excluirProduto(String sku) throws SQLException {
            String sql = "DELETE FROM tb_produtos WHERE sku = ?";
            try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, sku);
                ps.executeUpdate();
            }
        }

        public DefaultTableModel listarProdutos() throws SQLException {
            String sql = "SELECT nome_produto, sku, tipo_produto, modelo_licenca, preco_base, unidade_medida, custo_interno, categoria, status, data_lancamento FROM tb_produtos";
            DefaultTableModel modelo = new DefaultTableModel(
                    new String[]{"Nome", "SKU", "Tipo", "Modelo", "Preço", "Unidade", "Custo", "Categoria", "Status", "Data"}, 0);
            try (Connection c = conectar(); PreparedStatement stmt = c.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getString("nome_produto"),
                        rs.getString("sku"),
                        rs.getString("tipo_produto"),
                        rs.getString("modelo_licenca"),
                        rs.getDouble("preco_base"),
                        rs.getString("unidade_medida"),
                        rs.getDouble("custo_interno"),
                        rs.getString("categoria"),
                        rs.getString("status"),
                        rs.getDate("data_lancamento")
                    });
                }
            }
            return modelo;
        }
    }

    private final Conexao dao = new Conexao();
    private JTextField txtCliNome, txtCliEmail, txtCliTelefone;
    private JTable tabelaClientes;
    private JComboBox<String> cbContNomeCliente, cbRegNomeCliente;
    private JTextField txtContDescricao;
    private JTextField txtRegTitulo, txtRegValor;
    private JComboBox<String> cbRegStatus;
    private JTable tabelaContatos, tabelaRegistros, tabelaProdutos;
    private JTextField txtProdNome, txtProdSku, txtProdTipo, txtProdModelo;
    private JTextField txtProdPreco, txtProdUnidade, txtProdCusto, txtProdCategoria, txtProdData;
    private JComboBox<String> cbProdStatus;
    private JTabbedPane abas;

    public teste() {
        setTitle("CRM - Sistema Completo");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        abas = new JTabbedPane();
        abas.addTab("Clientes", criarAbaClientes());
        abas.addTab("Contatos", criarAbaContatos());
        abas.addTab("Registros", criarAbaRegistros());
        abas.addTab("Produtos", criarAbaProdutos());

        add(abas);
        listarTodos();
    }

    private JPanel criarAbaClientes() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 5, 5));

        txtCliNome = new JTextField();
        txtCliEmail = new JTextField();
        txtCliTelefone = new JTextField();

        JButton btnSalvar = new JButton("Salvar Novo Cliente");
        JButton btnExcluir = new JButton("Excluir Cliente");

        // NOVO BOTÃO DE ATUALIZAÇÃO
        JButton btnAtualizar = new JButton("Atualizar Nome");

        form.add(new JLabel("Nome:"));
        form.add(txtCliNome);
        form.add(new JLabel("Email:"));
        form.add(txtCliEmail);
        form.add(new JLabel("Telefone:"));
        form.add(txtCliTelefone);

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnExcluir);
        painelBotoes.add(btnAtualizar);
        form.add(painelBotoes);

        tabelaClientes = new JTable();
        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaClientes), BorderLayout.CENTER);

        tabelaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabelaClientes.getSelectedRow() != -1) {
                int linha = tabelaClientes.getSelectedRow();
                txtCliNome.setText(tabelaClientes.getValueAt(linha, 0).toString());
                txtCliEmail.setText(tabelaClientes.getValueAt(linha, 1).toString());
                txtCliTelefone.setText(tabelaClientes.getValueAt(linha, 2).toString());
            }
        });

        // LÓGICA DO NOVO BOTÃO DE ATUALIZAÇÃO
        btnAtualizar.addActionListener(ev -> {
            int linhaSelecionada = tabelaClientes.getSelectedRow();
            if (linhaSelecionada >= 0) {

                String nomeAtual = tabelaClientes.getValueAt(linhaSelecionada, 0).toString();

                String novoNome = txtCliNome.getText();

                try {
                    if (nomeAtual.equals(novoNome)) {
                        JOptionPane.showMessageDialog(this, "O nome digitado é o mesmo da seleção. Nenhuma alteração realizada.");
                        return;
                    }

                    dao.atualizarNomeCliente(nomeAtual, novoNome);
                    JOptionPane.showMessageDialog(this, "Nome do cliente atualizado para '" + novoNome + "'!");

                    listarTodos();
                    atualizarComboClientes();
                    limparCampos(txtCliNome, txtCliEmail, txtCliTelefone);

                } catch (Exception ex) {
                    erro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela para atualizar!");
            }
        });

        btnSalvar.addActionListener(ev -> {
            try {
                dao.inserirCliente(txtCliNome.getText(), txtCliEmail.getText(), txtCliTelefone.getText());
                JOptionPane.showMessageDialog(this, "Cliente salvo!");
                listarTodos();
                atualizarComboClientes();
                limparCampos(txtCliNome, txtCliEmail, txtCliTelefone);
                abas.setSelectedIndex(1);

            } catch (Exception ex) {
                erro(ex);
            }
        });

        btnExcluir.addActionListener(ev -> {
            int linhaSelecionada = tabelaClientes.getSelectedRow();
            if (linhaSelecionada >= 0) {
                String nome = tabelaClientes.getValueAt(linhaSelecionada, 0).toString();
                try {
                    dao.excluirRegistro(nome);
                    dao.excluirContato(nome);
                    dao.excluirCliente(nome);
                    JOptionPane.showMessageDialog(this, "Cliente e registros excluídos!");
                    listarTodos();
                    atualizarComboClientes();
                } catch (Exception ex) {
                    erro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um cliente para excluir!");
            }
        });

        return painel;
    }

    private JPanel criarAbaContatos() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));

        cbContNomeCliente = new JComboBox<>();
        txtContDescricao = new JTextField();
        JButton btnSalvar = new JButton("Salvar Contato");
        JButton btnExcluir = new JButton("Excluir Contato");

        form.add(new JLabel("Nome Cliente:"));
        form.add(cbContNomeCliente);
        form.add(new JLabel("Descrição:"));
        form.add(txtContDescricao);

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnExcluir);
        form.add(painelBotoes);

        tabelaContatos = new JTable();
        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaContatos), BorderLayout.CENTER);

        btnSalvar.addActionListener(ev -> {
            try {
                String nomeCliente = cbContNomeCliente.getSelectedItem().toString();
                dao.inserirContato(nomeCliente, txtContDescricao.getText());
                JOptionPane.showMessageDialog(this, "Contato registrado!");
                listarTodos();
                limparCampos(txtContDescricao);
                abas.setSelectedIndex(2);
            } catch (Exception ex) {
                erro(ex);
            }
        });

        btnExcluir.addActionListener(ev -> {
            int linhaSelecionada = tabelaContatos.getSelectedRow();
            if (linhaSelecionada >= 0) {
                String nomeCliente = tabelaContatos.getValueAt(linhaSelecionada, 0).toString();
                try {
                    dao.excluirContato(nomeCliente);
                    JOptionPane.showMessageDialog(this, "Contato excluído!");
                    listarTodos();
                } catch (Exception ex) {
                    erro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um contato para excluir!");
            }
        });

        return painel;
    }

    private JPanel criarAbaRegistros() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(5, 2, 5, 5));

        cbRegNomeCliente = new JComboBox<>();
        txtRegTitulo = new JTextField();
        txtRegValor = new JTextField();
        cbRegStatus = new JComboBox<>(new String[]{"em aberto", "negociando", "vendido", "perdido"});
        JButton btnSalvar = new JButton("Salvar Registro");
        JButton btnExcluir = new JButton("Excluir Registro");

        form.add(new JLabel("Nome Cliente:"));
        form.add(cbRegNomeCliente);
        form.add(new JLabel("Título:"));
        form.add(txtRegTitulo);
        form.add(new JLabel("Valor:"));
        form.add(txtRegValor);
        form.add(new JLabel("Status:"));
        form.add(cbRegStatus);

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnExcluir);
        form.add(painelBotoes);

        tabelaRegistros = new JTable();
        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaRegistros), BorderLayout.CENTER);

        btnSalvar.addActionListener(ev -> {
            try {
                String nomeCliente = cbRegNomeCliente.getSelectedItem().toString();
                dao.inserirRegistro(nomeCliente, Double.parseDouble(txtRegValor.getText()), txtRegTitulo.getText(), cbRegStatus.getSelectedItem().toString());
                JOptionPane.showMessageDialog(this, "Registro salvo!");
                listarTodos();
                limparCampos(txtRegTitulo, txtRegValor);
                abas.setSelectedIndex(3);
            } catch (Exception ex) {
                erro(ex);
            }
        });

        btnExcluir.addActionListener(ev -> {
            int linhaSelecionada = tabelaRegistros.getSelectedRow();
            if (linhaSelecionada >= 0) {
                String nomeCliente = tabelaRegistros.getValueAt(linhaSelecionada, 3).toString();
                try {
                    dao.excluirRegistro(nomeCliente);
                    JOptionPane.showMessageDialog(this, "Registro excluído!");
                    listarTodos();
                } catch (Exception ex) {
                    erro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um registro para excluir!");
            }
        });

        return painel;
    }

    private JPanel criarAbaProdutos() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(6, 4, 5, 5));

        txtProdNome = new JTextField();
        txtProdSku = new JTextField();
        txtProdTipo = new JTextField();
        txtProdModelo = new JTextField();
        txtProdPreco = new JTextField();
        txtProdUnidade = new JTextField();
        txtProdCusto = new JTextField();
        txtProdCategoria = new JTextField();
        txtProdData = new JTextField();
        cbProdStatus = new JComboBox<>(new String[]{"Ativo", "Descontinuado", "Beta"});
        JButton btnSalvar = new JButton("Salvar Produto");
        JButton btnExcluir = new JButton("Excluir Produto");

        form.add(new JLabel("Nome:"));
        form.add(txtProdNome);
        form.add(new JLabel("SKU:"));
        form.add(txtProdSku);
        form.add(new JLabel("Tipo:"));
        form.add(txtProdTipo);
        form.add(new JLabel("Modelo:"));
        form.add(txtProdModelo);
        form.add(new JLabel("Preço:"));
        form.add(txtProdPreco);
        form.add(new JLabel("Unidade:"));
        form.add(txtProdUnidade);
        form.add(new JLabel("Custo:"));
        form.add(txtProdCusto);
        form.add(new JLabel("Categoria:"));
        form.add(txtProdCategoria);
        form.add(new JLabel("Data Lançamento (YYYY-MM-DD):"));
        form.add(txtProdData);
        form.add(new JLabel("Status:"));
        form.add(cbProdStatus);

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnExcluir);
        form.add(painelBotoes);

        tabelaProdutos = new JTable();
        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaProdutos), BorderLayout.CENTER);

        btnSalvar.addActionListener(ev -> {
            try {
                Double custo = txtProdCusto.getText().isEmpty() ? null : Double.parseDouble(txtProdCusto.getText());
                dao.inserirProduto(txtProdNome.getText(), txtProdSku.getText(), txtProdTipo.getText(), txtProdModelo.getText(),
                        Double.parseDouble(txtProdPreco.getText()), txtProdUnidade.getText(), custo,
                        txtProdCategoria.getText(), cbProdStatus.getSelectedItem().toString(), txtProdData.getText());
                JOptionPane.showMessageDialog(this, "Produto salvo!");
                listarTodos();
                limparCampos(txtProdNome, txtProdSku, txtProdTipo, txtProdModelo, txtProdPreco, txtProdUnidade, txtProdCusto, txtProdCategoria, txtProdData);
                int resposta = JOptionPane.showConfirmDialog(this,
                        "Processo de cadastro finalizado. Deseja iniciar um NOVO CADASTRO DE CLIENTE e recomeçar o programa?",
                        "Reiniciar Fluxo",
                        JOptionPane.YES_NO_OPTION);

                if (resposta == JOptionPane.YES_OPTION) {
                    abas.setSelectedIndex(0);
                    limparCampos(txtCliNome, txtCliEmail, txtCliTelefone);
                    txtCliNome.requestFocus();
                }

            } catch (Exception ex) {
                erro(ex);
            }
        });

        btnExcluir.addActionListener(ev -> {
            int linhaSelecionada = tabelaProdutos.getSelectedRow();
            if (linhaSelecionada >= 0) {
                String sku = tabelaProdutos.getValueAt(linhaSelecionada, 1).toString();
                try {
                    dao.excluirProduto(sku);
                    JOptionPane.showMessageDialog(this, "Produto excluído!");
                    listarTodos();
                } catch (Exception ex) {
                    erro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um produto para excluir!");
            }
        });

        return painel;
    }

    private void listarTodos() {
        try {
            tabelaClientes.setModel(dao.listarClientes());
            tabelaContatos.setModel(dao.listarContatos());
            tabelaRegistros.setModel(dao.listarRegistros());
            tabelaProdutos.setModel(dao.listarProdutos());
        } catch (Exception ex) {
            erro(ex);
        }
    }

    private void atualizarComboClientes() {
        try {
            DefaultTableModel modelo = dao.listarClientes();
            cbContNomeCliente.removeAllItems();
            cbRegNomeCliente.removeAllItems();
            for (int i = 0; i < modelo.getRowCount(); i++) {
                String nome = modelo.getValueAt(i, 0).toString();
                cbContNomeCliente.addItem(nome);
                cbRegNomeCliente.addItem(nome);
            }
        } catch (Exception ex) {
            erro(ex);
        }
    }

    private void limparCampos(JTextField... campos) {
        for (JTextField campo : campos) {
            campo.setText("");
        }
    }

    private void erro(Exception ex) {
        JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new teste().setVisible(true));
    }
    
}
